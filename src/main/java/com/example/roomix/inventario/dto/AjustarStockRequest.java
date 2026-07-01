package com.example.roomix.inventario.dto;

import com.example.roomix.inventario.domain.TipoMovimientoStock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "AjustarStockRequest", description = "Movimiento de entrada o salida de stock")
public record AjustarStockRequest(
        @Schema(description = "Tipo de movimiento", example = "ENTRADA")
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoStock tipo,

        @Schema(description = "Cantidad a mover (siempre positiva)", example = "5", minimum = "1")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        @Schema(description = "Motivo del movimiento (opcional)", example = "Reposición semanal", nullable = true)
        @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
        String motivo
) {
}
