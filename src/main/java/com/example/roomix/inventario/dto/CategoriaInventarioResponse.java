package com.example.roomix.inventario.dto;

import com.example.roomix.inventario.domain.CategoriaInventario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(name = "CategoriaInventarioResponse", description = "Categoría del inventario hotelero")
@Builder
public record CategoriaInventarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Limpieza") String nombre,
        @Schema(example = "Productos y herramientas para la limpieza del hotel") String descripcion,
        @Schema(example = "Artículos de aseo, trapeadores, paños, escobas") String ejemplosArticulos,
        @Schema(example = "true") boolean activo,
        @Schema(description = "Categoría precargada por el sistema", example = "true") boolean predefinida,
        @Schema(type = "string", format = "date-time") LocalDateTime fechaHoraCreacion,
        @Schema(type = "string", format = "date-time") LocalDateTime fechaHoraUltimaActualizacion
) {
    public static CategoriaInventarioResponse from(CategoriaInventario categoria) {
        return CategoriaInventarioResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .ejemplosArticulos(categoria.getEjemplosArticulos())
                .activo(categoria.isActivo())
                .predefinida(categoria.isPredefinida())
                .fechaHoraCreacion(categoria.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(categoria.getFechaHoraUltimaActualizacion())
                .build();
    }
}
