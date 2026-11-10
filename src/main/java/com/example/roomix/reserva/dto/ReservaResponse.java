package com.example.roomix.reserva.dto;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.huesped.dto.HuespedResumenResponse;
import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.domain.Reserva;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "ReservaResponse")
@Builder
public record ReservaResponse(
        Long id,
        Long habitacionId,
        String habitacionNumero,
        TipoHabitacion tipoHabitacion,
        HuespedResumenResponse huesped,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        Integer cantidadNoches,
        EstadoReserva estadoReserva,
        BigDecimal tarifaNoche,
        BigDecimal totalEstimado,
        LocalDateTime horaRealCheckIn,
        LocalDateTime horaRealCheckOut,
        CanalReserva canal,
        String canalNombre,
        String observaciones,
        String motivoCancelacion,
        LocalDateTime fechaHoraCancelacion,
        String registradaPor,
        LocalDateTime fechaHoraCreacion,
        LocalDateTime fechaHoraUltimaActualizacion,
        @Schema(description = "Noches × tarifa, antes del descuento") BigDecimal subtotal,
        BigDecimal descuento,
        Long promocionId,
        String promocionNombre,
        @Schema(description = "Parte del abono retenida como penalidad al cancelar") BigDecimal valorPenalidad,
        @Schema(description = "Parte del abono devuelta al huésped al cancelar") BigDecimal valorDevuelto
) {
    public static ReservaResponse from(Reserva reserva) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .habitacionId(reserva.getHabitacion().getId())
                .habitacionNumero(reserva.getHabitacion().getNumero())
                .tipoHabitacion(reserva.getHabitacion().getTipoHabitacion())
                .huesped(HuespedResumenResponse.from(reserva.getHuesped()))
                .fechaEntrada(reserva.getFechaEntrada())
                .fechaSalida(reserva.getFechaSalida())
                .cantidadNoches(reserva.getCantidadNoches())
                .estadoReserva(reserva.getEstadoReserva())
                .tarifaNoche(reserva.getTarifaNoche())
                .totalEstimado(reserva.getTotalEstimado())
                .horaRealCheckIn(reserva.getHoraRealCheckIn())
                .horaRealCheckOut(reserva.getHoraRealCheckOut())
                .canal(reserva.getCanal())
                .canalNombre(reserva.getCanal() != null ? reserva.getCanal().getNombre() : null)
                .observaciones(reserva.getObservaciones())
                .motivoCancelacion(reserva.getMotivoCancelacion())
                .fechaHoraCancelacion(reserva.getFechaHoraCancelacion())
                .registradaPor(reserva.getRegistradaPor())
                .subtotal(reserva.getTarifaNoche().multiply(BigDecimal.valueOf(reserva.getCantidadNoches())))
                .descuento(reserva.getDescuento() != null ? reserva.getDescuento() : BigDecimal.ZERO)
                .promocionId(reserva.getPromocion() != null ? reserva.getPromocion().getId() : null)
                .promocionNombre(reserva.getPromocion() != null ? reserva.getPromocion().getNombre() : null)
                .valorPenalidad(reserva.getValorPenalidad())
                .valorDevuelto(reserva.getValorDevuelto())
                .fechaHoraCreacion(reserva.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(reserva.getFechaHoraUltimaActualizacion())
                .build();
    }
}
