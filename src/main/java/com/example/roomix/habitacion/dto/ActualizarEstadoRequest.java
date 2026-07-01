package com.example.roomix.habitacion.dto;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.MotivoInhabilitacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(name = "ActualizarEstadoRequest")
public record ActualizarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoHabitacion estado,

        @Schema(description = "Fecha planificada de entrada al reservar", format = "date", nullable = true)
        LocalDate fechaEntrada,

        @Schema(description = "Fecha planificada de salida al reservar", format = "date", nullable = true)
        LocalDate fechaSalida,

        @Schema(description = "Obligatorio al pasar a INHABILITADO", nullable = true)
        MotivoInhabilitacion motivoInhabilitacion,

        @Schema(description = "Obligatorio al reservar la habitación", nullable = true)
        Long huespedId
) {
}
