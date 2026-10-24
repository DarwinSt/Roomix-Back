package com.example.roomix.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "ResumenInventarioResponse", description = "Reporte de inventario: valorización, críticos y consumo")
public record ResumenInventarioResponse(
        int articulosActivos,
        @Schema(description = "Suma de cantidad × precio unitario") BigDecimal valorTotal,
        @Schema(description = "Artículos activos sin precio de referencia (no suman a la valorización)") int articulosSinPrecio,
        int totalCriticos,
        List<ArticuloInventarioResponse> criticos,
        List<ValorCategoria> valorPorCategoria,
        int diasAnalizados,
        List<ArticuloConsumo> masUsados
) {
    @Schema(name = "ValorCategoriaInventario")
    public record ValorCategoria(String categoria, long articulos, long unidades, BigDecimal valor) {
    }

    @Schema(name = "ArticuloConsumo")
    public record ArticuloConsumo(Long articuloId, String nombre, String unidadMedida, long unidadesSalida,
                                  BigDecimal valorConsumido) {
    }
}
