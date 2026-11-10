package com.example.roomix.finanzas.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.config.HotelProperties;
import com.example.roomix.finanzas.domain.AbonoReserva;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.Pago;
import com.example.roomix.finanzas.dto.AbonoDtos.AbonoRequest;
import com.example.roomix.finanzas.dto.AbonoDtos.AbonoResponse;
import com.example.roomix.finanzas.dto.AbonoDtos.ResumenAbonos;
import com.example.roomix.finanzas.exception.FinanzasErrorCode;
import com.example.roomix.finanzas.exception.FinanzasException;
import com.example.roomix.finanzas.repository.AbonoReservaRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.domain.ReservaCanceladaEvent;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.reserva.service.PoliticaReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Abonos de las reservas: abono mínimo para reservar, abonos posteriores, aplicación como pago de la factura
 * en el check-out y devolución con penalidad cuando la reserva se cancela.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbonoService {

    private static final NumberFormat PESOS = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    private final AbonoReservaRepository abonoRepository;
    private final ReservaRepository reservaRepository;
    private final MovimientoFinancieroRepository movimientoRepository;
    private final FinanzasService finanzasService;
    private final PoliticaReservaService politicaService;
    private final HotelProperties hotel;
    private final com.example.roomix.habitacion.repository.HabitacionRepository habitacionRepository;
    private final com.example.roomix.huesped.repository.HuespedRepository huespedRepository;
    private final com.example.roomix.reserva.service.PromocionService promocionService;
    private final com.example.roomix.reserva.service.TarifaService tarifaService;

    /** Cotiza la reserva con la promoción escogida y el IVA del huésped (si aún no existe, se asume IVA). */
    public com.example.roomix.finanzas.dto.AbonoDtos.Cotizacion cotizar(Long habitacionId, java.time.LocalDate entrada,
                                                                         java.time.LocalDate salida, Long promocionId,
                                                                         com.example.roomix.reserva.domain.CanalReserva canal,
                                                                         Long huespedId) {
        var habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new com.example.roomix.habitacion.exception.HabitacionNotFoundException(habitacionId));
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new ReservaException(ReservaErrorCode.FECHAS_INVALIDAS);
        }
        int noches = (int) java.time.temporal.ChronoUnit.DAYS.between(entrada, salida);
        BigDecimal tarifa = habitacion.getPrecioNoche() != null ? habitacion.getPrecioNoche()
                : tarifaService.obtenerPrecioNoche(habitacion.getTipoHabitacion());
        BigDecimal subtotal = tarifa.multiply(BigDecimal.valueOf(noches));
        BigDecimal descuento = BigDecimal.ZERO;
        String promocion = null;
        if (promocionId != null) {
            var p = promocionService.validarAplicable(promocionId, habitacion, noches, canal, entrada);
            descuento = p.descuentoSobre(subtotal);
            promocion = p.getNombre();
        }
        BigDecimal total = subtotal.subtract(descuento);
        var huesped = huespedId != null ? huespedRepository.findById(huespedId).orElse(null) : null;
        BigDecimal porcentaje = ReglaIva.porcentaje(hotel, huesped);
        BigDecimal impuesto = ReglaIva.impuesto(total, porcentaje);
        BigDecimal conImpuesto = total.add(impuesto);
        var politica = politicaService.vigente();
        BigDecimal minimo = conImpuesto.multiply(politica.getAbonoMinimoPct()).divide(BigDecimal.valueOf(100), 0, RoundingMode.UP);
        return new com.example.roomix.finanzas.dto.AbonoDtos.Cotizacion(noches, tarifa, subtotal, descuento, promocion, total,
                porcentaje, impuesto, conImpuesto, politica.getAbonoMinimoPct(), minimo, politica.getPenalidadCancelacionPct());
    }

    /** Total que pagará el huésped por el alojamiento: total estimado (ya con descuento) más IVA si aplica. */
    public BigDecimal totalReserva(Reserva r) {
        BigDecimal porcentaje = ReglaIva.porcentaje(hotel, r.getHuesped());
        return r.getTotalEstimado().add(ReglaIva.impuesto(r.getTotalEstimado(), porcentaje));
    }

    /** Abono mínimo según la política vigente, redondeado al peso hacia arriba. */
    public BigDecimal abonoMinimo(Reserva r) {
        BigDecimal pct = politicaService.vigente().getAbonoMinimoPct();
        return totalReserva(r).multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.UP);
    }

    public ResumenAbonos resumen(Long reservaId) {
        Reserva r = buscarReserva(reservaId);
        List<AbonoReserva> abonos = abonoRepository.findByReservaIdOrderByFechaHoraAsc(reservaId);
        BigDecimal total = totalReserva(r);
        BigDecimal abonado = abonos.stream().filter(a -> !a.isDevuelto()).map(AbonoReserva::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenAbonos(r.getId(), total, ReglaIva.porcentaje(hotel, r.getHuesped()),
                politicaService.vigente().getAbonoMinimoPct(), abonoMinimo(r), abonado,
                r.getEstadoReserva() == EstadoReserva.CANCELADA ? BigDecimal.ZERO : total.subtract(abonado).max(BigDecimal.ZERO),
                r.getValorPenalidad(), r.getValorDevuelto(), abonos.stream().map(AbonoResponse::from).toList());
    }

    /** Al crear la reserva: exige el abono mínimo de la política y registra el abono inicial. */
    @Transactional
    public void registrarAbonoInicial(Long reservaId, AbonoRequest abono) {
        Reserva r = buscarReserva(reservaId);
        BigDecimal minimo = abonoMinimo(r);
        BigDecimal valor = abono != null && abono.valor() != null ? abono.valor() : BigDecimal.ZERO;
        // Toda reserva exige un pago al crearla: completo o un abono parcial (al menos el mínimo de la política)
        if (valor.signum() <= 0) {
            throw new ReservaException(ReservaErrorCode.ABONO_INSUFICIENTE,
                    "Para reservar debe registrar el pago: completo (" + PESOS.format(totalReserva(r)) + ") o un abono"
                            + (minimo.signum() > 0 ? " de al menos " + PESOS.format(minimo) : ""));
        }
        if (minimo.signum() > 0 && valor.compareTo(minimo) < 0) {
            throw new ReservaException(ReservaErrorCode.ABONO_INSUFICIENTE,
                    "Para reservar se requiere un abono mínimo de " + PESOS.format(minimo) + " ("
                            + politicaService.vigente().getAbonoMinimoPct().stripTrailingZeros().toPlainString()
                            + " % del total de " + PESOS.format(totalReserva(r)) + ")");
        }
        registrar(reservaId, abono);
    }

    @Transactional
    public AbonoResponse registrar(Long reservaId, AbonoRequest request) {
        Reserva r = buscarReserva(reservaId);
        if (r.getEstadoReserva() != EstadoReserva.CONFIRMADA && r.getEstadoReserva() != EstadoReserva.EN_CURSO) {
            throw new FinanzasException(FinanzasErrorCode.RESERVA_NO_ABONABLE, r.getEstadoReserva());
        }
        BigDecimal valor = request.valor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal abonado = abonoRepository.findByReservaIdOrderByFechaHoraAsc(reservaId).stream()
                .filter(a -> !a.isDevuelto()).map(AbonoReserva::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = totalReserva(r).subtract(abonado);
        if (valor.compareTo(saldo) > 0) {
            throw new FinanzasException(FinanzasErrorCode.ABONO_EXCEDE_TOTAL, PESOS.format(valor), PESOS.format(saldo.max(BigDecimal.ZERO)));
        }
        AbonoReserva abono = abonoRepository.save(AbonoReserva.builder()
                .reserva(r)
                .valor(valor)
                .metodo(request.metodo())
                .referencia(StringUtils.hasText(request.referencia()) ? request.referencia().trim() : null)
                .fechaHora(LocalDateTime.now())
                .registradoPor(UsuarioActual.username())
                .build());
        MovimientoFinanciero ingreso = finanzasService.registrarAutomatico(CategoriaFinanciera.ALOJAMIENTO,
                "Abono de la reserva #" + r.getId() + " — habitación " + r.getHabitacion().getNumero()
                        + " (" + r.getHuesped().nombreCompleto() + ")",
                valor, request.metodo(), r.getHuesped().nombreCompleto(), "RES-" + r.getId(), null, null);
        ingreso.setAbonoId(abono.getId());
        movimientoRepository.save(ingreso);
        return AbonoResponse.from(abono);
    }

    /** Abonos aún no facturados ni devueltos (cuenta del huésped antes del check-out). */
    public BigDecimal abonadoSinAplicar(Long reservaId) {
        return abonoRepository.findByReservaIdOrderByFechaHoraAsc(reservaId).stream()
                .filter(AbonoReserva::vigente).map(AbonoReserva::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * En la facturación de la reserva, los abonos vigentes se aplican como pagos de la factura. No generan un nuevo
     * ingreso: ya se registraron como ingreso el día en que se recibieron.
     */
    @Transactional
    public void aplicarAFactura(Factura factura) {
        if (factura.getReserva() == null) {
            return;
        }
        for (AbonoReserva abono : abonoRepository.findByReservaIdOrderByFechaHoraAsc(factura.getReserva().getId())) {
            if (!abono.vigente() || factura.saldo().signum() <= 0) {
                continue;
            }
            BigDecimal valor = abono.getValor().min(factura.saldo());
            factura.getPagos().add(Pago.builder()
                    .factura(factura)
                    .valor(valor)
                    .metodo(abono.getMetodo())
                    .referencia("Abono #" + abono.getId() + (abono.getReferencia() != null ? " · " + abono.getReferencia() : ""))
                    .fechaHora(abono.getFechaHora())
                    .registradoPor(abono.getRegistradoPor())
                    .build());
            factura.setTotalPagado(factura.getTotalPagado().add(valor));
            factura.setEstado(factura.saldo().signum() == 0 ? EstadoFactura.PAGADA : EstadoFactura.PAGADA_PARCIAL);
            abono.setFacturaId(factura.getId());
            abonoRepository.save(abono);
        }
    }

    /**
     * Cancelación de una reserva con abonos: se retiene la penalidad de la política (porcentaje de lo abonado) y el
     * resto se devuelve al huésped como egreso de devolución.
     */
    @EventListener
    @Transactional
    public void alCancelarReserva(ReservaCanceladaEvent evento) {
        Reserva r = buscarReserva(evento.reservaId());
        List<AbonoReserva> vigentes = abonoRepository.findByReservaIdOrderByFechaHoraAsc(r.getId()).stream()
                .filter(AbonoReserva::vigente).toList();
        BigDecimal abonado = vigentes.stream().map(AbonoReserva::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (abonado.signum() == 0) {
            return;
        }
        BigDecimal pct = politicaService.vigente().getPenalidadCancelacionPct();
        BigDecimal retenido = abonado.multiply(pct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).min(abonado);
        BigDecimal devolver = abonado.subtract(retenido);
        LocalDateTime ahora = LocalDateTime.now();
        BigDecimal pendiente = devolver;
        for (int i = 0; i < vigentes.size(); i++) {
            AbonoReserva a = vigentes.get(i);
            BigDecimal parte = i == vigentes.size() - 1
                    ? pendiente
                    : devolver.multiply(a.getValor()).divide(abonado, 2, RoundingMode.HALF_UP);
            pendiente = pendiente.subtract(parte);
            a.setDevuelto(true);
            a.setValorDevuelto(parte);
            a.setFechaHoraDevolucion(ahora);
            abonoRepository.save(a);
        }
        if (devolver.signum() > 0) {
            finanzasService.registrarAutomatico(CategoriaFinanciera.OTROS_EGRESOS,
                    "Devolución de abonos de la reserva #" + r.getId() + " cancelada (penalidad retenida "
                            + pct.stripTrailingZeros().toPlainString() + " %: " + PESOS.format(retenido) + ")",
                    devolver, vigentes.getLast().getMetodo(), r.getHuesped().nombreCompleto(), "RES-" + r.getId(), null, null);
        }
        r.setValorPenalidad(retenido);
        r.setValorDevuelto(devolver);
        reservaRepository.save(r);
    }

    private Reserva buscarReserva(Long id) {
        return reservaRepository.findById(id).orElseThrow(() -> new ReservaException(ReservaErrorCode.RESERVA_NO_ENCONTRADA));
    }
}
