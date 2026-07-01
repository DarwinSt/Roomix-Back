package com.example.roomix.inventario.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de movimiento de stock en inventario")
public enum TipoMovimientoStock {

    @Schema(description = "Ingreso de unidades al inventario")
    ENTRADA,

    @Schema(description = "Salida de unidades del inventario")
    SALIDA
}
