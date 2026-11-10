package com.example.roomix.finanzas.dto;

import com.example.roomix.finanzas.domain.AbonoReserva;
import com.example.roomix.finanzas.domain.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AbonoDtos {

    private AbonoDtos() {
    }

    @Schema(name = "AbonoRequest")
    public record AbonoRequest(
            @NotNull(message = "El valor del abono es obligatorio")
            @DecimalMin(value = "0.01", message = "El abono debe ser mayor que cero") BigDecimal valor,
            @NotNull(message = "El medio de pago es obligatorio") MetodoPago metodo,
            @Size(max = 80) String referencia
    ) {
    }

    @Schema(name = "AbonoResponse")
    public record AbonoResponse(Long id, BigDecimal valor, MetodoPago metodo, String metodoNombre, String referencia,
                                LocalDateTime fechaHora, String registradoPor, Long facturaId, boolean devuelto,
                                BigDecimal valorDevuelto, LocalDateTime fechaHoraDevolucion) {
        public static AbonoResponse from(AbonoReserva a) {
            return new AbonoResponse(a.getId(), a.getValor(), a.getMetodo(), a.getMetodo().getNombre(), a.getReferencia(),
                    a.getFechaHora(), a.getRegistradoPor(), a.getFacturaId(), a.isDevuelto(), a.getValorDevuelto(),
                    a.getFechaHoraDevolucion());
        }
    }

    /** Cotización de una reserva antes de crearla: la misma regla que aplicará el backend al confirmar. */
    @Schema(name = "CotizacionReservaResponse")
    public record Cotizacion(
            int noches,
            BigDecimal tarifaNoche,
            BigDecimal subtotal,
            BigDecimal descuento,
            String promocionNombre,
            BigDecimal total,
            BigDecimal porcentajeImpuesto,
            BigDecimal impuesto,
            BigDecimal totalConImpuesto,
            BigDecimal abonoMinimoPct,
            BigDecimal abonoMinimo,
            BigDecimal penalidadCancelacionPct
    ) {
    }

    /** Estado de los abonos de una reserva frente a su total (alojamiento con descuento e impuestos). */
    @Schema(name = "ResumenAbonosResponse")
    public record ResumenAbonos(
            Long reservaId,
            @Schema(description = "Alojamiento (con descuento) más IVA") BigDecimal totalReserva,
            BigDecimal porcentajeImpuesto,
            BigDecimal abonoMinimoPct,
            BigDecimal abonoMinimo,
            @Schema(description = "Suma de los abonos no devueltos") BigDecimal totalAbonado,
            BigDecimal saldo,
            BigDecimal valorPenalidad,
            BigDecimal valorDevuelto,
            List<AbonoResponse> abonos
    ) {
    }
}
