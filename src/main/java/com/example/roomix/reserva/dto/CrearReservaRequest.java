package com.example.roomix.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.roomix.finanzas.dto.AbonoDtos.AbonoRequest;
import com.example.roomix.reserva.domain.CanalReserva;
import jakarta.validation.Valid;
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
        @Size(max = 500) String observaciones,
        @Schema(description = "Promoción escogida (una por reserva); opcional") Long promocionId,
        @Valid @Schema(description = "Abono inicial; obligatorio si la política exige un abono mínimo") AbonoRequest abonoInicial
) {
    /** Compatibilidad con los llamados que no envían promoción ni abono. */
    public CrearReservaRequest(Long habitacionId, Long huespedId, LocalDate fechaEntrada, LocalDate fechaSalida,
                               CanalReserva canal, String observaciones) {
        this(habitacionId, huespedId, fechaEntrada, fechaSalida, canal, observaciones, null, null);
    }
}
