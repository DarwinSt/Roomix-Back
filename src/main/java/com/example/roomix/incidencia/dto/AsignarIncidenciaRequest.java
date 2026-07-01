package com.example.roomix.incidencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AsignarIncidenciaRequest")
public record AsignarIncidenciaRequest(
        @NotNull
        @Schema(description = "ID del personal asignado", example = "1")
        Long personalId
) {
}
