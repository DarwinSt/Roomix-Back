package com.example.roomix.inventario.dto;

import com.example.roomix.inventario.domain.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(name = "ArticuloInventarioRequest", description = "Datos para crear o actualizar un artículo de inventario")
@Builder
public record ArticuloInventarioRequest(
        @Schema(description = "Nombre del artículo", example = "Toalla de baño", maxLength = 120)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Schema(description = "Descripción del artículo", example = "Toalla blanca 70x140 cm", maxLength = 500)
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @Schema(description = "ID de la categoría de inventario", example = "1")
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Schema(description = "Cantidad actual en inventario", example = "50", minimum = "0")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer cantidad,

        @Schema(description = "Umbral mínimo para alerta de stock bajo", example = "10", minimum = "0", nullable = true)
        @Min(value = 0, message = "La cantidad mínima no puede ser negativa")
        Integer cantidadMinima,

        @Schema(description = "Unidad de medida", example = "UNIDAD")
        @NotNull(message = "La unidad de medida es obligatoria")
        UnidadMedida unidadMedida,

        @Schema(description = "Ubicación física en el hotel", example = "Almacén planta baja - estante A3", nullable = true)
        @Size(max = 150, message = "La ubicación no puede superar 150 caracteres")
        String ubicacion,

        @Schema(description = "Indica si el artículo está activo", example = "true")
        Boolean activo
) {
}
