package com.example.roomix.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(name = "CrearReservaRequest")
public record CrearReservaRequest(
        @NotNull Long habitacionId,
        @NotNull Long huespedId,
        @NotNull LocalDate fechaEntrada,
        @NotNull LocalDate fechaSalida
) {
}
