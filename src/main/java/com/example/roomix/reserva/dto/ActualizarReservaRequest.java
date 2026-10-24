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
        @Size(max = 500) String observaciones
) {
}
