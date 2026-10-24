package com.example.roomix.personal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "PersonalRequest")
public record PersonalRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite máximo 120 caracteres")
        String nombre,

        @NotBlank(message = "El cargo es obligatorio")
        @Size(max = 80, message = "El cargo admite máximo 80 caracteres")
        @Schema(example = "Camarera de pisos")
        String rol,

        @Size(max = 80, message = "El departamento admite máximo 80 caracteres")
        @Schema(example = "Housekeeping")
        String departamento,

        Boolean activo
) {
}
