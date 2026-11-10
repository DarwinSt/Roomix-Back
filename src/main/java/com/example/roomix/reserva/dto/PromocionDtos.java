package com.example.roomix.reserva.dto;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.domain.PoliticaReserva;
import com.example.roomix.reserva.domain.Promocion;
import com.example.roomix.reserva.domain.Promocion.TipoDescuento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PromocionDtos {

    private PromocionDtos() {
    }

    @Schema(name = "PromocionRequest")
    public record PromocionRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @Size(max = 300) String descripcion,
            @Min(value = 1, message = "El mínimo de noches debe ser al menos 1") Integer minimoNoches,
            TipoHabitacion tipoHabitacion,
            CanalReserva canal,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            @NotNull(message = "El tipo de descuento es obligatorio") TipoDescuento tipoDescuento,
            @NotNull(message = "El valor del descuento es obligatorio")
            @DecimalMin(value = "0.01", message = "El descuento debe ser mayor que cero") BigDecimal valor,
            Boolean activa
    ) {
    }

    @Schema(name = "PromocionResponse")
    public record PromocionResponse(
            Long id,
            String nombre,
            String descripcion,
            Integer minimoNoches,
            TipoHabitacion tipoHabitacion,
            CanalReserva canal,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            TipoDescuento tipoDescuento,
            BigDecimal valor,
            boolean activa,
            String condiciones,
            LocalDateTime fechaHoraUltimaActualizacion
    ) {
        public static PromocionResponse from(Promocion p) {
            return new PromocionResponse(p.getId(), p.getNombre(), p.getDescripcion(), p.getMinimoNoches(), p.getTipoHabitacion(),
                    p.getCanal(), p.getVigenteDesde(), p.getVigenteHasta(), p.getTipoDescuento(), p.getValor(), p.isActiva(),
                    condiciones(p), p.getFechaHoraUltimaActualizacion());
        }

        private static String condiciones(Promocion p) {
            var partes = new java.util.ArrayList<String>();
            if (p.getMinimoNoches() != null) {
                partes.add(p.getMinimoNoches() + " noches o más");
            }
            if (p.getTipoHabitacion() != null) {
                partes.add("habitación " + p.getTipoHabitacion().name().toLowerCase());
            }
            if (p.getCanal() != null) {
                partes.add("canal " + p.getCanal().getNombre());
            }
            if (p.getVigenteDesde() != null || p.getVigenteHasta() != null) {
                partes.add("llegadas " + (p.getVigenteDesde() != null ? "desde el " + p.getVigenteDesde() : "")
                        + (p.getVigenteHasta() != null ? " hasta el " + p.getVigenteHasta() : ""));
            }
            return partes.isEmpty() ? "Todas las reservas" : String.join(" · ", partes);
        }
    }

    /** Promoción que aplica a una reserva concreta, con el descuento ya calculado. */
    @Schema(name = "PromocionAplicableResponse")
    public record PromocionAplicableResponse(
            Long id,
            String nombre,
            String descripcion,
            String condiciones,
            TipoDescuento tipoDescuento,
            BigDecimal valor,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal totalConDescuento
    ) {
    }

    @Schema(name = "PoliticaReservaRequest")
    public record PoliticaReservaRequest(
            @NotNull @DecimalMin("0") @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
            @Schema(description = "Porcentaje del total que se debe abonar para crear la reserva (0 = sin mínimo)") BigDecimal abonoMinimoPct,
            @NotNull @DecimalMin("0") @DecimalMax(value = "100", message = "El porcentaje no puede superar 100")
            @Schema(description = "Porcentaje de lo abonado que se retiene al cancelar") BigDecimal penalidadCancelacionPct
    ) {
    }

    @Schema(name = "PoliticaReservaResponse")
    public record PoliticaReservaResponse(BigDecimal abonoMinimoPct, BigDecimal penalidadCancelacionPct, String actualizadaPor,
                                          LocalDateTime fechaHoraUltimaActualizacion) {
        public static PoliticaReservaResponse from(PoliticaReserva p) {
            return new PoliticaReservaResponse(p.getAbonoMinimoPct(), p.getPenalidadCancelacionPct(), p.getActualizadaPor(),
                    p.getFechaHoraUltimaActualizacion());
        }
    }
}
