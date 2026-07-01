package com.example.roomix.inventario.dto;

import com.example.roomix.inventario.domain.ArticuloInventario;
import com.example.roomix.inventario.domain.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(name = "ArticuloInventarioResponse", description = "Artículo de inventario del hotel")
@Builder
public record ArticuloInventarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Toalla de baño") String nombre,
        @Schema(example = "Toalla blanca 70x140 cm") String descripcion,
        @Schema(description = "Categoría del artículo") CategoriaInventarioResponse categoria,
        @Schema(example = "50") Integer cantidad,
        @Schema(example = "10", nullable = true) Integer cantidadMinima,
        @Schema(example = "UNIDAD") UnidadMedida unidadMedida,
        @Schema(example = "Almacén planta baja", nullable = true) String ubicacion,
        @Schema(example = "true") boolean activo,
        @Schema(description = "Cantidad en o por debajo del mínimo", example = "false")
        boolean stockBajo,
        @Schema(type = "string", format = "date-time") LocalDateTime fechaHoraCreacion,
        @Schema(type = "string", format = "date-time") LocalDateTime fechaHoraUltimaActualizacion
) {
    public static ArticuloInventarioResponse from(ArticuloInventario articulo) {
        return ArticuloInventarioResponse.builder()
                .id(articulo.getId())
                .nombre(articulo.getNombre())
                .descripcion(articulo.getDescripcion())
                .categoria(CategoriaInventarioResponse.from(articulo.getCategoria()))
                .cantidad(articulo.getCantidad())
                .cantidadMinima(articulo.getCantidadMinima())
                .unidadMedida(articulo.getUnidadMedida())
                .ubicacion(articulo.getUbicacion())
                .activo(articulo.isActivo())
                .stockBajo(calcularStockBajo(articulo))
                .fechaHoraCreacion(articulo.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(articulo.getFechaHoraUltimaActualizacion())
                .build();
    }

    private static boolean calcularStockBajo(ArticuloInventario articulo) {
        if (articulo.getCantidadMinima() == null) {
            return false;
        }
        return articulo.getCantidad() <= articulo.getCantidadMinima();
    }
}
