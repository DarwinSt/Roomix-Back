package com.example.roomix.incidencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(name = "FinalizarIncidenciaRequest", description = "Materiales del inventario gastados (opcional)")
public record FinalizarIncidenciaRequest(@Valid List<MaterialRequest> materiales) {

    public record MaterialRequest(
            @NotNull(message = "Indique el artículo") Long articuloId,
            @NotNull(message = "Indique la cantidad") @Min(value = 1, message = "La cantidad debe ser mayor que cero") Integer cantidad
    ) {
    }
}
