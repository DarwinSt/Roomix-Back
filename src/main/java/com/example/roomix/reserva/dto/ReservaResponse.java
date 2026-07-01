package com.example.roomix.reserva.dto;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.huesped.dto.HuespedResumenResponse;
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
        LocalDateTime fechaHoraCreacion,
        LocalDateTime fechaHoraUltimaActualizacion
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
                .fechaHoraCreacion(reserva.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(reserva.getFechaHoraUltimaActualizacion())
                .build();
    }
}
