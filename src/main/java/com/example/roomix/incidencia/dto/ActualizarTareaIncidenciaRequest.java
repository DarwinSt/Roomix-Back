package com.example.roomix.incidencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "ActualizarTareaIncidenciaRequest")
public record ActualizarTareaIncidenciaRequest(
        @NotNull
        @Schema(description = "Indica si la tarea quedó completada", example = "true")
        Boolean completada
) {
}
