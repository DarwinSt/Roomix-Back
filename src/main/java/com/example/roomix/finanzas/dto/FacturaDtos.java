package com.example.roomix.finanzas.dto;

import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import com.example.roomix.finanzas.domain.FacturaItem;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.OrigenItem;
import com.example.roomix.finanzas.domain.Pago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FacturaDtos {

    private FacturaDtos() {
    }

    @Schema(name = "LineaCuenta")
    public record LineaCuenta(
            String descripcion,
            BigDecimal cantidad,
            BigDecimal valorUnitario,
            BigDecimal total,
            OrigenItem origen,
            Long referenciaId,
            boolean facturado
    ) {
    }

    @Schema(name = "CuentaReservaResponse", description = "Estado de cuenta del huésped para una reserva")
    public record CuentaResponse(
            Long reservaId,
            String habitacionNumero,
            String huespedNombre,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            List<LineaCuenta> lineas,
            BigDecimal subtotal,
            BigDecimal porcentajeImpuesto,
            BigDecimal impuesto,
            BigDecimal total,
            boolean exentoIva,
            BigDecimal totalPagado,
            BigDecimal saldo,
            List<FacturaResponse> facturas
    ) {
    }

    @Schema(name = "PagoRequest")
    public record PagoRequest(
            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero")
            BigDecimal valor,
            @NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,
            @Size(max = 80) String referencia
    ) {
    }

    @Schema(name = "FacturaItemResponse")
    public record ItemResponse(Long id, String descripcion, BigDecimal cantidad, BigDecimal valorUnitario,
                               BigDecimal total, OrigenItem origen) {
        static ItemResponse from(FacturaItem i) {
            return new ItemResponse(i.getId(), i.getDescripcion(), i.getCantidad(), i.getValorUnitario(), i.getTotal(), i.getOrigen());
        }
    }

    @Schema(name = "PagoResponse")
    public record PagoResponse(Long id, BigDecimal valor, MetodoPago metodo, String metodoNombre, String referencia,
                               LocalDateTime fechaHora, String registradoPor) {
        static PagoResponse from(Pago p) {
            return new PagoResponse(p.getId(), p.getValor(), p.getMetodo(), p.getMetodo().getNombre(), p.getReferencia(),
                    p.getFechaHora(), p.getRegistradoPor());
        }
    }

    @Schema(name = "FacturaResponse")
    public record FacturaResponse(
            Long id,
            String numero,
            Long reservaId,
            String habitacionNumero,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            Long eventoId,
            Long huespedId,
            String clienteNombre,
            String clienteDocumento,
            String clienteEmail,
            LocalDateTime fechaEmision,
            BigDecimal subtotal,
            BigDecimal porcentajeImpuesto,
            BigDecimal impuesto,
            BigDecimal total,
            BigDecimal totalPagado,
            BigDecimal saldo,
            EstadoFactura estado,
            String observaciones,
            String motivoAnulacion,
            String emitidaPor,
            List<ItemResponse> items,
            List<PagoResponse> pagos
    ) {
        public static FacturaResponse from(Factura f) {
            var reserva = f.getReserva();
            return new FacturaResponse(
                    f.getId(), f.getNumero(),
                    reserva != null ? reserva.getId() : null,
                    reserva != null ? reserva.getHabitacion().getNumero() : null,
                    reserva != null ? reserva.getFechaEntrada() : null,
                    reserva != null ? reserva.getFechaSalida() : null,
                    f.getEventoId(),
                    f.getHuesped() != null ? f.getHuesped().getId() : null,
                    f.getClienteNombre(), f.getClienteDocumento(), f.getClienteEmail(),
                    f.getFechaEmision(), f.getSubtotal(), f.getPorcentajeImpuesto(), f.getImpuesto(), f.getTotal(),
                    f.getTotalPagado(), f.saldo(), f.getEstado(), f.getObservaciones(), f.getMotivoAnulacion(),
                    f.getEmitidaPor(),
                    f.getItems().stream().map(ItemResponse::from).toList(),
                    f.getPagos().stream().map(PagoResponse::from).toList()
            );
        }
    }
}
