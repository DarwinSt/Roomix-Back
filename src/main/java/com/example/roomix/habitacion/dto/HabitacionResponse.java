package com.example.roomix.habitacion.dto;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.MotivoInhabilitacion;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.huesped.dto.HuespedResumenResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "HabitacionResponse")
@Builder
public record HabitacionResponse(
        Long id,
        String numero,
        List<String> caracteristicas,
        TipoHabitacion tipoHabitacion,
        String descripcion,
        EstadoHabitacion estado,
        @Schema(description = "Fecha planificada de entrada (reserva)", format = "date", nullable = true)
        LocalDate fechaEntrada,
        @Schema(description = "Fecha planificada de salida (reserva)", format = "date", nullable = true)
        LocalDate fechaSalida,
        @Schema(description = "Noches reservadas; no cambia con el check-in tardío", nullable = true)
        Integer cantidadNoches,
        @Schema(description = "Estado de la reserva planificada", nullable = true)
        EstadoReserva estadoReserva,
        @Schema(description = "Hora real de check-in del huésped", format = "date-time", nullable = true)
        LocalDateTime horaRealCheckIn,
        @Schema(description = "Hora real de check-out del huésped", format = "date-time", nullable = true)
        LocalDateTime horaRealCheckOut,
        MotivoInhabilitacion motivoInhabilitacion,
        @Schema(description = "Huésped asignado a la reserva o estadía actual", nullable = true)
        HuespedResumenResponse huesped,
        LocalDateTime fechaHoraUltimaActualizacion
) {
    public static HabitacionResponse from(Habitacion habitacion) {
        return HabitacionResponse.builder()
                .id(habitacion.getId())
                .numero(habitacion.getNumero())
                .caracteristicas(List.copyOf(habitacion.getCaracteristicas()))
                .tipoHabitacion(habitacion.getTipoHabitacion())
                .descripcion(habitacion.getDescripcion())
                .estado(habitacion.getEstado())
                .fechaEntrada(habitacion.getFechaEntrada())
                .fechaSalida(habitacion.getFechaSalida())
                .cantidadNoches(habitacion.getCantidadNoches())
                .estadoReserva(habitacion.getEstadoReserva())
                .horaRealCheckIn(habitacion.getHoraRealCheckIn())
                .horaRealCheckOut(habitacion.getHoraRealCheckOut())
                .motivoInhabilitacion(habitacion.getMotivoInhabilitacion())
                .huesped(HuespedResumenResponse.from(habitacion.getHuesped()))
                .fechaHoraUltimaActualizacion(habitacion.getFechaHoraUltimaActualizacion())
                .build();
    }
}
