package com.example.roomix.auditoria.service;

import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import com.example.roomix.venta.domain.EstadoVenta;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.repository.VentaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Anulaciones para revisión de la administración: facturas, ventas y movimientos de dinero anulados, con el motivo,
 * quién los registró y quién los anuló. Marca cuando la misma persona anuló algo que ella registró.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnulacionesService {

    private final FacturaRepository facturaRepository;
    private final MovimientoFinancieroRepository movimientoRepository;
    private final VentaProductoRepository ventaRepository;

    public record Anulacion(
            String tipo,
            Long id,
            String referencia,
            String descripcion,
            BigDecimal valor,
            String motivo,
            String registradoPor,
            String anuladoPor,
            LocalDateTime fechaHora,
            boolean mismaPersona
    ) {
    }

    public List<Anulacion> listar(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        LocalDateTime d = inicio.atStartOfDay();
        LocalDateTime h = fin.plusDays(1).atStartOfDay();
        List<Anulacion> lista = new ArrayList<>();

        facturaRepository.findByEstadoAndFechaHoraAnulacionBetween(EstadoFactura.ANULADA, d, h).forEach(f ->
                lista.add(anulacion("FACTURA", f.getId(), f.getNumero(), f.getClienteNombre(), f.getTotal(), f.getMotivoAnulacion(),
                        f.getEmitidaPor(), f.getAnuladaPor(), f.getFechaHoraAnulacion())));

        ventaRepository.findByEstadoAndFechaHoraAnulacionBetween(EstadoVenta.ANULADA, d, h).forEach(v ->
                lista.add(anulacion("VENTA", v.getId(), "Venta #" + v.getId(),
                        v.getItems().stream().map(i -> i.getNombre() + " ×" + i.getCantidad()).collect(Collectors.joining(", "))
                                + (v.getModo() == ModoVenta.CARGO_HABITACION && v.getReserva() != null
                                ? " · Hab. " + v.getReserva().getHabitacion().getNumero() : ""),
                        v.getTotal(), v.getMotivoAnulacion(), v.getRegistradaPor(), v.getAnuladaPor(), v.getFechaHoraAnulacion())));

        movimientoRepository.findByAnuladoTrueAndFechaHoraAnulacionBetween(d, h).stream()
                .filter(AnulacionesService::esManual)
                .forEach(m -> lista.add(anulacion(m.getTipo().name(), m.getId(), m.getCategoria().getNombre(), m.getConcepto(),
                        m.getValor(), m.getMotivoAnulacion(), m.getRegistradoPor(), m.getAnuladoPor(), m.getFechaHoraAnulacion())));

        lista.sort(Comparator.comparing(Anulacion::fechaHora, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    /**
     * Movimientos que se anularon por sí mismos. Los de pagos de facturas y los de ventas ya aparecen con su factura o su
     * venta, para no contarlos dos veces.
     */
    private static boolean esManual(MovimientoFinanciero m) {
        return m.getFacturaId() == null && m.getPagoId() == null && m.getAbonoId() == null
                && (m.getReferencia() == null || !m.getReferencia().startsWith("VTA-"));
    }

    private static Anulacion anulacion(String tipo, Long id, String referencia, String descripcion, BigDecimal valor, String motivo,
                                       String registradoPor, String anuladoPor, LocalDateTime fecha) {
        boolean misma = registradoPor != null && registradoPor.equalsIgnoreCase(anuladoPor);
        return new Anulacion(tipo, id, referencia, descripcion, valor, motivo, registradoPor, anuladoPor, fecha, misma);
    }
}
