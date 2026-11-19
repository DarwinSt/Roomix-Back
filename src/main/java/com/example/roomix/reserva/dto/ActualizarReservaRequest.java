package com.example.roomix.reserva.dto;

import com.example.roomix.reserva.domain.CanalReserva;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "ActualizarReservaRequest",
        description = "Reserva confirmada: se puede cambiar todo. Estadía en curso: solo la fecha de salida y las observaciones.")
public record ActualizarReservaRequest(
        Long huespedId,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate fechaEntrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate fechaSalida,
        CanalReserva canal,
        @Size(max = 500) String observaciones,
        @jakarta.validation.constraints.Min(value = 1, message = "Debe haber al menos una persona")
        @jakarta.validation.constraints.Max(value = 100, message = "Número de personas no válido")
        @Schema(description = "Personas de la estadía; si se omite se conserva") Integer cantidadPersonas
) {
    public ActualizarReservaRequest(Long huespedId, LocalDate fechaEntrada, LocalDate fechaSalida, CanalReserva canal,
                                    String observaciones) {
        this(huespedId, fechaEntrada, fechaSalida, canal, observaciones, null);
    }
}
