package com.example.roomix.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.roomix.reserva.domain.CanalReserva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "CrearReservaRequest")
public record CrearReservaRequest(
        @NotNull Long habitacionId,
        @NotNull Long huespedId,
        @NotNull LocalDate fechaEntrada,
        @NotNull LocalDate fechaSalida,
        @Schema(description = "Canal de la reserva; por defecto PRESENCIAL") CanalReserva canal,
        @Size(max = 500) String observaciones
) {
}
