package com.example.roomix.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
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
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        Integer cantidad,

        @Schema(description = "Motivo del movimiento (opcional)", example = "Reposición semanal", nullable = true)
        @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
        String motivo,

        @Schema(description = "Costo unitario de la entrada (actualiza el precio de referencia)")
        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        BigDecimal costoUnitario,

        @Schema(description = "En una ENTRADA, registrar la compra como egreso en el módulo financiero")
        Boolean registrarCompra,

        @Size(max = 120) String proveedor
) {
}
