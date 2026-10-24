package com.example.roomix.finanzas.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.config.HotelProperties;
import com.example.roomix.evento.domain.Evento;
import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoRecurso;
import com.example.roomix.evento.service.EventoService;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import com.example.roomix.finanzas.domain.FacturaItem;
import com.example.roomix.finanzas.domain.OrigenItem;
import com.example.roomix.finanzas.domain.Pago;
import com.example.roomix.finanzas.dto.FacturaDtos.CuentaResponse;
import com.example.roomix.finanzas.dto.FacturaDtos.FacturaResponse;
import com.example.roomix.finanzas.dto.FacturaDtos.LineaCuenta;
import com.example.roomix.finanzas.dto.FacturaDtos.PagoRequest;
import com.example.roomix.finanzas.exception.FinanzasErrorCode;
import com.example.roomix.finanzas.exception.FinanzasException;
import com.example.roomix.finanzas.repository.FacturaItemRepository;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.servicio.domain.SolicitudServicio;
import com.example.roomix.servicio.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuenta del huésped, emisión de facturas (reservas y eventos), pagos y anulaciones.
 * Cada pago genera automáticamente los ingresos en el módulo financiero, por categoría.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacturacionService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final FacturaRepository facturaRepository;
    private final FacturaItemRepository facturaItemRepository;
    private final ReservaRepository reservaRepository;
    private final ServicioService servicioService;
    private final EventoService eventoService;
    private final FinanzasService finanzasService;
    private final HotelProperties hotel;

    // ------------------------------------------------------------------ Consultas

    public PaginaResponse<FacturaResponse> listar(EstadoFactura estado, LocalDate desde, LocalDate hasta, String texto,
                                                  int pagina, int tamano) {
        return PaginaResponse.de(
                facturaRepository.findAll(
                        FacturaRepository.conFiltros(estado,
                                desde != null ? desde.atStartOfDay() : null,
                                hasta != null ? hasta.plusDays(1).atStartOfDay() : null,
                                texto),
                        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100),
                                Sort.by(Sort.Direction.DESC, "fechaEmision", "id"))),
                FacturaResponse::from
        );
    }

    public FacturaResponse obtener(Long id) {
        return FacturaResponse.from(buscar(id));
    }

    public List<FacturaResponse> deHuesped(Long huespedId) {
        return facturaRepository.findByHuespedIdOrderByFechaEmisionDesc(huespedId).stream()
                .map(FacturaResponse::from).toList();
    }

    /** Estado de cuenta: alojamiento + servicios, con lo ya facturado y pagado. */
    public CuentaResponse cuentaReserva(Long reservaId) {
        Reserva reserva = buscarReserva(reservaId);
        List<LineaCuenta> lineas = new ArrayList<>();
        lineas.add(lineaAlojamiento(reserva, facturaItemRepository.estaFacturado(OrigenItem.ALOJAMIENTO, reserva.getId())));
        for (SolicitudServicio s : servicioService.cobrablesDeReserva(reserva.getId())) {
            lineas.add(lineaServicio(s, facturaItemRepository.estaFacturado(OrigenItem.SERVICIO, s.getId())));
        }

        boolean exento = exentoIva(reserva.getHuesped());
        BigDecimal porcentaje = exento ? BigDecimal.ZERO : hotel.ivaPorcentaje();
        BigDecimal subtotal = lineas.stream().map(LineaCuenta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impuesto = calcularImpuesto(subtotal, porcentaje);

        List<Factura> facturas = facturasVigentesDeReserva(reserva.getId());
        BigDecimal pagado = facturas.stream().map(Factura::getTotalPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.add(impuesto);
        return new CuentaResponse(
                reserva.getId(),
                reserva.getHabitacion().getNumero(),
                reserva.getHuesped().nombreCompleto(),
                reserva.getFechaEntrada(),
                reserva.getFechaSalida(),
                lineas,
                subtotal,
                porcentaje,
                impuesto,
                total,
                exento,
                pagado,
                total.subtract(pagado),
                facturas.stream().map(FacturaResponse::from).toList()
        );
    }

    // ------------------------------------------------------------------ Emisión

    /**
     * Factura la reserva (alojamiento + servicios no facturados). Si existe una factura sin pagos,
     * se actualiza con los nuevos consumos; si todo ya está facturado, devuelve la última factura.
     */
    @Transactional
    public FacturaResponse facturarReserva(Long reservaId) {
        Reserva reserva = buscarReserva(reservaId);
        if (reserva.getEstadoReserva() != EstadoReserva.EN_CURSO && reserva.getEstadoReserva() != EstadoReserva.FINALIZADA) {
            throw new FinanzasException(FinanzasErrorCode.RESERVA_NO_FACTURABLE, reserva.getEstadoReserva());
        }

        Factura editable = liberarFacturaSinPagos(facturasVigentesDeReserva(reserva.getId()));

        List<FacturaItem> items = new ArrayList<>();
        if (!facturaItemRepository.estaFacturado(OrigenItem.ALOJAMIENTO, reserva.getId())) {
            items.add(item(lineaAlojamiento(reserva, false)));
        }
        for (SolicitudServicio s : servicioService.cobrablesDeReserva(reserva.getId())) {
            if (!facturaItemRepository.estaFacturado(OrigenItem.SERVICIO, s.getId())) {
                items.add(item(lineaServicio(s, false)));
            }
        }

        if (items.isEmpty()) {
            List<Factura> vigentes = facturasVigentesDeReserva(reserva.getId());
            if (!vigentes.isEmpty()) {
                return FacturaResponse.from(vigentes.getLast());
            }
            throw new FinanzasException(FinanzasErrorCode.FACTURA_SIN_ITEMS);
        }

        Huesped huesped = reserva.getHuesped();
        Factura factura = editable != null ? editable : Factura.builder()
                .reserva(reserva)
                .huesped(huesped)
                .clienteNombre(huesped.nombreCompleto())
                .clienteDocumento(huesped.getTipoDocumento().name() + " " + huesped.getNumeroDocumento())
                .clienteEmail(huesped.getEmail())
                .emitidaPor(UsuarioActual.username())
                .build();
        BigDecimal porcentaje = exentoIva(huesped) ? BigDecimal.ZERO : hotel.ivaPorcentaje();
        String observacion = porcentaje.signum() == 0
                ? "Exento de IVA: servicio turístico prestado a no residente (art. 481 lit. d, E.T.)"
                : null;
        return FacturaResponse.from(guardar(factura, items, porcentaje, observacion));
    }

    @Transactional
    public FacturaResponse facturarEvento(Long eventoId) {
        Evento evento = eventoService.buscar(eventoId);
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new FinanzasException(FinanzasErrorCode.FACTURA_SIN_ITEMS);
        }
        Factura existente = facturaRepository.findFirstByEventoIdAndEstadoNot(eventoId, EstadoFactura.ANULADA).orElse(null);
        if (existente != null && existente.getTotalPagado().signum() > 0) {
            return FacturaResponse.from(existente);
        }
        if (existente != null) {
            existente.getItems().clear();
            facturaRepository.flush();
        }

        List<FacturaItem> items = new ArrayList<>();
        items.add(FacturaItem.builder()
                .descripcion("Evento \"" + evento.getNombre() + "\" — " + evento.getSalon().getNombre() + " ("
                        + evento.getFechaHoraInicio().format(DIA_HORA) + " a " + evento.getFechaHoraFin().format(DIA_HORA) + ")")
                .cantidad(BigDecimal.ONE)
                .valorUnitario(evento.getValorBase())
                .total(evento.getValorBase())
                .origen(OrigenItem.EVENTO)
                .referenciaId(evento.getId())
                .build());
        for (EventoRecurso r : evento.getRecursos()) {
            items.add(FacturaItem.builder()
                    .descripcion(r.getTipo().getNombre() + ": " + r.getDescripcion())
                    .cantidad(BigDecimal.valueOf(r.getCantidad()))
                    .valorUnitario(r.getValorUnitario())
                    .total(r.getTotal())
                    .origen(OrigenItem.EVENTO)
                    .referenciaId(evento.getId())
                    .build());
        }

        Factura factura = existente != null ? existente : Factura.builder()
                .eventoId(evento.getId())
                .clienteNombre(evento.getOrganizadorNombre())
                .clienteDocumento(evento.getOrganizadorDocumento())
                .clienteEmail(evento.getOrganizadorEmail())
                .emitidaPor(UsuarioActual.username())
                .build();
        return FacturaResponse.from(guardar(factura, items, hotel.ivaPorcentaje(), null));
    }

    // ------------------------------------------------------------------ Pagos y anulación

    @Transactional
    public FacturaResponse registrarPago(Long facturaId, PagoRequest request) {
        Factura factura = buscar(facturaId);
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new FinanzasException(FinanzasErrorCode.FACTURA_ANULADA, factura.getNumero());
        }
        if (factura.getEstado() == EstadoFactura.PAGADA) {
            throw new FinanzasException(FinanzasErrorCode.FACTURA_PAGADA, factura.getNumero());
        }
        BigDecimal valor = request.valor().setScale(2, RoundingMode.HALF_UP);
        if (valor.compareTo(factura.saldo()) > 0) {
            throw new FinanzasException(FinanzasErrorCode.PAGO_EXCEDE_SALDO, valor, factura.saldo());
        }

        Pago pago = Pago.builder()
                .factura(factura)
                .valor(valor)
                .metodo(request.metodo())
                .referencia(StringUtils.hasText(request.referencia()) ? request.referencia().trim() : null)
                .fechaHora(LocalDateTime.now())
                .registradoPor(UsuarioActual.username())
                .build();
        factura.getPagos().add(pago);
        factura.setTotalPagado(factura.getTotalPagado().add(valor));
        factura.setEstado(factura.saldo().signum() == 0 ? EstadoFactura.PAGADA : EstadoFactura.PAGADA_PARCIAL);
        // flush (no save/merge): la factura ya está gestionada y así el pago persistido es esta misma
        // instancia, con su id asignado para enlazar los ingresos
        facturaRepository.flush();

        registrarIngresos(factura, pago);
        return FacturaResponse.from(factura);
    }

    @Transactional
    public FacturaResponse anular(Long facturaId, String motivo) {
        Factura factura = buscar(facturaId);
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new FinanzasException(FinanzasErrorCode.FACTURA_ANULADA, factura.getNumero());
        }
        String detalle = "Anulación de " + factura.getNumero() + ": " + motivo.trim();
        for (Pago pago : factura.getPagos()) {
            finanzasService.anularPorPago(pago.getId(), detalle);
        }
        factura.setEstado(EstadoFactura.ANULADA);
        factura.setMotivoAnulacion(motivo.trim());
        return FacturaResponse.from(facturaRepository.save(factura));
    }

    // ------------------------------------------------------------------ Internos

    /** Reparte el pago entre las categorías de ingreso según el peso de cada tipo de línea. */
    private void registrarIngresos(Factura factura, Pago pago) {
        Map<OrigenItem, BigDecimal> porOrigen = new EnumMap<>(OrigenItem.class);
        for (FacturaItem item : factura.getItems()) {
            porOrigen.merge(item.getOrigen(), item.getTotal(), BigDecimal::add);
        }
        BigDecimal restante = pago.getValor();
        List<Map.Entry<OrigenItem, BigDecimal>> grupos = new ArrayList<>(porOrigen.entrySet());
        for (int i = 0; i < grupos.size(); i++) {
            var grupo = grupos.get(i);
            BigDecimal parte = i == grupos.size() - 1
                    ? restante
                    : pago.getValor().multiply(grupo.getValue()).divide(factura.getSubtotal(), 2, RoundingMode.HALF_UP);
            restante = restante.subtract(parte);
            if (parte.signum() <= 0) {
                continue;
            }
            finanzasService.registrarAutomatico(
                    grupo.getKey().getCategoria(),
                    "Pago " + factura.getNumero() + " — " + grupo.getKey().getCategoria().getNombre()
                            + " (" + factura.getClienteNombre() + ")",
                    parte,
                    pago.getMetodo(),
                    factura.getClienteNombre(),
                    factura.getNumero(),
                    factura.getId(),
                    pago.getId()
            );
        }
    }

    private Factura guardar(Factura factura, List<FacturaItem> items, BigDecimal porcentaje, String observacion) {
        BigDecimal subtotal = items.stream().map(FacturaItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impuesto = calcularImpuesto(subtotal, porcentaje);
        items.forEach(i -> i.setFactura(factura));
        factura.getItems().addAll(items);
        factura.setSubtotal(subtotal);
        factura.setPorcentajeImpuesto(porcentaje);
        factura.setImpuesto(impuesto);
        factura.setTotal(subtotal.add(impuesto));
        factura.setFechaEmision(LocalDateTime.now());
        factura.setObservaciones(observacion);
        factura.setEstado(EstadoFactura.PENDIENTE);

        boolean nueva = factura.getId() == null;
        if (nueva) {
            factura.setNumero("TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        Factura guardada = facturaRepository.saveAndFlush(factura);
        if (nueva) {
            guardada.setNumero("%s-%06d".formatted(hotel.prefijoFactura(), guardada.getId()));
            guardada = facturaRepository.save(guardada);
        }
        return guardada;
    }

    /** Vacía una factura vigente sin pagos para volver a armarla con los consumos actuales. */
    private Factura liberarFacturaSinPagos(List<Factura> vigentes) {
        for (Factura f : vigentes) {
            if (f.getTotalPagado().signum() == 0) {
                f.getItems().clear();
                facturaRepository.flush();
                return f;
            }
        }
        return null;
    }

    private List<Factura> facturasVigentesDeReserva(Long reservaId) {
        return facturaRepository.findAll(
                (root, q, cb) -> cb.and(
                        cb.equal(root.get("reserva").get("id"), reservaId),
                        cb.notEqual(root.get("estado"), EstadoFactura.ANULADA)),
                Sort.by("id"));
    }

    private LineaCuenta lineaAlojamiento(Reserva r, boolean facturado) {
        String descripcion = "Alojamiento habitación " + r.getHabitacion().getNumero()
                + " — " + r.getCantidadNoches() + " noche(s) del " + r.getFechaEntrada().format(DIA)
                + " al " + r.getFechaSalida().format(DIA);
        return new LineaCuenta(descripcion, BigDecimal.valueOf(r.getCantidadNoches()), r.getTarifaNoche(),
                r.getTotalEstimado(), OrigenItem.ALOJAMIENTO, r.getId(), facturado);
    }

    private LineaCuenta lineaServicio(SolicitudServicio s, boolean facturado) {
        LocalDateTime fecha = s.getFechaHoraProgramada() != null ? s.getFechaHoraProgramada() : s.getFechaHoraSolicitud();
        return new LineaCuenta("Servicio: " + s.getServicio().getNombre() + " (" + fecha.format(DIA_HORA) + ")",
                BigDecimal.valueOf(s.getCantidad()), s.getPrecioUnitario(), s.getTotal(), OrigenItem.SERVICIO,
                s.getId(), facturado);
    }

    private static FacturaItem item(LineaCuenta l) {
        return FacturaItem.builder()
                .descripcion(l.descripcion())
                .cantidad(l.cantidad())
                .valorUnitario(l.valorUnitario())
                .total(l.total())
                .origen(l.origen())
                .referenciaId(l.referenciaId())
                .build();
    }

    /** Turista extranjero no residente (pasaporte y nacionalidad distinta a Colombia). */
    boolean exentoIva(Huesped huesped) {
        if (!hotel.exentoIvaExtranjeros() || huesped == null) {
            return false;
        }
        String nacionalidad = huesped.getNacionalidad() == null ? "" : huesped.getNacionalidad().trim();
        return huesped.getTipoDocumento() == TipoDocumento.PASAPORTE
                && !nacionalidad.isEmpty()
                && !nacionalidad.equalsIgnoreCase("Colombia");
    }

    private static BigDecimal calcularImpuesto(BigDecimal subtotal, BigDecimal porcentaje) {
        return subtotal.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private Factura buscar(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new FinanzasException(FinanzasErrorCode.FACTURA_NO_ENCONTRADA, id));
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new FinanzasException(FinanzasErrorCode.FACTURA_NO_ENCONTRADA, "reserva " + id));
    }
}
