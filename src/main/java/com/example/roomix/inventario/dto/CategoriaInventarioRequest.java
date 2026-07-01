package com.example.roomix.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(name = "CategoriaInventarioRequest", description = "Datos para crear o actualizar una categoría de inventario")
@Builder
public record CategoriaInventarioRequest(
        @Schema(description = "Nombre de la categoría", example = "Spa", maxLength = 80)
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @Schema(
                description = "Descripción de qué tipo de artículos agrupa",
                example = "Productos del área de spa y bienestar",
                maxLength = 300
        )
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 300, message = "La descripción no puede superar 300 caracteres")
        String descripcion,

        @Schema(
                description = "Ejemplos de artículos que pertenecen a esta categoría",
                example = "Toallas spa, aceites, velas aromáticas, batas",
                maxLength = 500
        )
        @NotBlank(message = "Los ejemplos de artículos son obligatorios")
        @Size(max = 500, message = "Los ejemplos no pueden superar 500 caracteres")
        String ejemplosArticulos,

        @Schema(description = "Si la categoría está disponible para nuevos artículos", example = "true")
        Boolean activo
) {
}
