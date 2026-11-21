package com.example.roomix.venta.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.OrigenItem;
import com.example.roomix.finanzas.repository.FacturaItemRepository;
import com.example.roomix.finanzas.service.FinanzasService;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.inventario.domain.ArticuloInventario;
import com.example.roomix.inventario.domain.TipoMovimientoStock;
import com.example.roomix.inventario.dto.AjustarStockRequest;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import com.example.roomix.inventario.service.InventarioService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.venta.domain.EstadoVenta;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.domain.VentaProducto;
import com.example.roomix.venta.domain.VentaProductoItem;
import com.example.roomix.venta.dto.VentaDtos.AnularVentaRequest;
import com.example.roomix.venta.dto.VentaDtos.EstadiaActiva;
import com.example.roomix.venta.dto.VentaDtos.ItemVentaRequest;
import com.example.roomix.venta.dto.VentaDtos.ProductoVenta;
import com.example.roomix.venta.dto.VentaDtos.VentaRequest;
import com.example.roomix.venta.dto.VentaDtos.VentaResponse;
import com.example.roomix.venta.exception.VentaErrorCode;
import com.example.roomix.venta.exception.VentaException;
import com.example.roomix.venta.repository.VentaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Venta de productos del inventario a huéspedes (gaseosas, snacks, artículos de aseo…). Cada venta descuenta el stock y:
 * <ul>
 *   <li>de contado: registra el ingreso en Finanzas y en la caja del turno, con su medio de pago;</li>
 *   <li>cargada a la habitación: queda en la cuenta del huésped y se cobra con la factura en el check-out.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaService {

    private final VentaProductoRepository ventaRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final InventarioService inventarioService;
    private final ReservaRepository reservaRepository;
    private final FinanzasService finanzasService;
    private final FacturaItemRepository facturaItemRepository;

    /** Productos a la venta (activos y con precio de venta), por categoría y nombre. */
    public List<ProductoVenta> catalogo() {
        return articuloRepository.findAllWithCategoria().stream()
                .filter(ArticuloInventario::seVende)
                .sorted(Comparator.comparing((ArticuloInventario a) -> a.getCategoria().getNombre())
                        .thenComparing(ArticuloInventario::getNombre))
                .map(a -> new ProductoVenta(a.getId(), a.getNombre(), a.getCategoria().getNombre(), a.getPrecioVenta(),
                        a.getCantidad(), a.getUnidadMedida().name()))
                .toList();
    }

    /** Huéspedes alojados, a cuyas habitaciones se pueden cargar consumos. */
    public List<EstadiaActiva> estadiasActivas() {
        return reservaRepository.findByEstadoReservaOrderByHabitacionNumeroAsc(EstadoReserva.EN_CURSO).stream()
                .map(r -> new EstadiaActiva(r.getId(), r.getHabitacion().getNumero(), r.getHuesped().nombreCompleto(),
                        r.getFechaEntrada(), r.getFechaSalida()))
                .toList();
    }

    public List<VentaResponse> listar(Long reservaId, LocalDate fecha) {
        List<VentaProducto> ventas = reservaId != null
                ? ventaRepository.findByReservaIdOrderByFechaHoraDesc(reservaId)
                : ventaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(
                (fecha != null ? fecha : LocalDate.now()).atStartOfDay(),
                (fecha != null ? fecha : LocalDate.now()).plusDays(1).atStartOfDay());
        return ventas.stream().map(VentaResponse::from).toList();
    }

    @Transactional
    public VentaResponse registrar(VentaRequest request) {
        Reserva reserva = null;
        if (request.modo() == ModoVenta.CARGO_HABITACION) {
            if (request.reservaId() == null) {
                throw new VentaException(VentaErrorCode.RESERVA_REQUERIDA);
            }
            reserva = reservaRepository.findById(request.reservaId())
                    .orElseThrow(() -> new VentaException(VentaErrorCode.RESERVA_REQUERIDA));
            if (reserva.getEstadoReserva() != EstadoReserva.EN_CURSO) {
                throw new VentaException(VentaErrorCode.RESERVA_NO_EN_CURSO, reserva.getId(),
                        reserva.getEstadoReserva().name().toLowerCase().replace('_', ' '));
            }
        } else if (request.metodoPago() == null) {
            throw new VentaException(VentaErrorCode.METODO_PAGO_REQUERIDO);
        }

        // El mismo producto en varias líneas se suma, para validar el stock una sola vez
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemVentaRequest i : request.items()) {
            cantidades.merge(i.articuloId(), i.cantidad(), Integer::sum);
        }

        VentaProducto venta = VentaProducto.builder()
                .modo(request.modo())
                .reserva(reserva)
                .cliente(texto(request.cliente()))
                .metodoPago(request.modo() == ModoVenta.CONTADO ? request.metodoPago() : null)
                .referencia(texto(request.referencia()))
                .total(BigDecimal.ZERO)
                .registradaPor(UsuarioActual.username())
                .fechaHora(LocalDateTime.now())
                .build();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            ArticuloInventario a = articuloRepository.findById(e.getKey())
                    .orElseThrow(() -> new VentaException(VentaErrorCode.PRODUCTO_NO_ENCONTRADO, e.getKey()));
            if (!a.seVende()) {
                throw new VentaException(VentaErrorCode.PRODUCTO_NO_VENDIBLE, a.getNombre());
            }
            if (a.getCantidad() < e.getValue()) {
                throw new VentaException(VentaErrorCode.STOCK_INSUFICIENTE, a.getNombre(), a.getCantidad(), e.getValue());
            }
            BigDecimal subtotal = a.getPrecioVenta().multiply(BigDecimal.valueOf(e.getValue())).setScale(2, RoundingMode.HALF_UP);
            venta.getItems().add(VentaProductoItem.builder()
                    .venta(venta)
                    .articuloId(a.getId())
                    .nombre(a.getNombre())
                    .cantidad(e.getValue())
                    .precioUnitario(a.getPrecioVenta())
                    .total(subtotal)
                    .build());
            total = total.add(subtotal);
        }
        venta.setTotal(total);
        venta = ventaRepository.saveAndFlush(venta);

        String motivo = "Venta #" + venta.getId() + (reserva != null ? " — habitación " + reserva.getHabitacion().getNumero() : "");
        for (VentaProductoItem i : venta.getItems()) {
            inventarioService.ajustarStock(i.getArticuloId(),
                    new AjustarStockRequest(TipoMovimientoStock.SALIDA, i.getCantidad(), motivo, null, false, null));
        }

        if (venta.getModo() == ModoVenta.CONTADO) {
            // Entra a Finanzas y a la caja del turno (la recepción necesita su turno abierto)
            MovimientoFinanciero ingreso = finanzasService.registrarAutomatico(CategoriaFinanciera.VENTA_PRODUCTOS,
                    "Venta de productos #" + venta.getId() + ": " + resumen(venta), total, venta.getMetodoPago(),
                    venta.getCliente(), referenciaVenta(venta), null, null);
            venta.setMovimientoFinancieroId(ingreso.getId());
        }
        return VentaResponse.from(venta);
    }

    /** Anula la venta: devuelve los productos al inventario y reversa el ingreso (o quita el cargo de la cuenta). */
    @Transactional
    public VentaResponse anular(Long id, AnularVentaRequest request) {
        VentaProducto venta = ventaRepository.findById(id)
                .orElseThrow(() -> new VentaException(VentaErrorCode.VENTA_NO_ENCONTRADA, id));
        if (!venta.vigente()) {
            throw new VentaException(VentaErrorCode.VENTA_YA_ANULADA, id);
        }
        if (venta.getModo() == ModoVenta.CARGO_HABITACION
                && venta.getItems().stream().anyMatch(i -> facturaItemRepository.estaFacturado(OrigenItem.PRODUCTO, i.getId()))) {
            throw new VentaException(VentaErrorCode.VENTA_FACTURADA, id);
        }
        String motivo = request.motivo().trim();
        if (venta.getMovimientoFinancieroId() != null) {
            String nota = "Venta #" + id + " anulada: " + motivo;
            finanzasService.anular(venta.getMovimientoFinancieroId(), nota.length() > 200 ? nota.substring(0, 200) : nota);
        }
        for (VentaProductoItem i : venta.getItems()) {
            inventarioService.ajustarStock(i.getArticuloId(), new AjustarStockRequest(TipoMovimientoStock.ENTRADA, i.getCantidad(),
                    "Anulación de la venta #" + id, null, false, null));
        }
        venta.setEstado(EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(motivo);
        venta.setAnuladaPor(UsuarioActual.username());
        venta.setFechaHoraAnulacion(LocalDateTime.now());
        return VentaResponse.from(ventaRepository.save(venta));
    }

    /** Productos cargados a la cuenta de una estadía (los usa la facturación). */
    public List<VentaProductoItem> cargosDeReserva(Long reservaId) {
        return ventaRepository.itemsCargados(reservaId, ModoVenta.CARGO_HABITACION, EstadoVenta.VIGENTE);
    }

    private static String resumen(VentaProducto v) {
        String texto = v.getItems().stream().map(i -> i.getNombre() + " ×" + i.getCantidad()).collect(Collectors.joining(", "));
        return texto.length() > 150 ? texto.substring(0, 147) + "…" : texto;
    }

    private static String referenciaVenta(VentaProducto v) {
        String r = v.getReferencia() != null ? "VTA-" + v.getId() + " " + v.getReferencia() : "VTA-" + v.getId();
        return r.length() > 60 ? r.substring(0, 60) : r;
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
