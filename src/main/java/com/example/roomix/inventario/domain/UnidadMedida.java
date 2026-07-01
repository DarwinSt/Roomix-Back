package com.example.roomix.inventario.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unidad en la que se cuenta el artículo")
public enum UnidadMedida {

    @Schema(description = "Pieza individual")
    UNIDAD,

    @Schema(description = "Par de elementos")
    PAR,

    @Schema(description = "Juego o set")
    JUEGO,

    @Schema(description = "Caja o paquete")
    CAJA,

    @Schema(description = "Litros (líquidos)")
    LITRO,

    @Schema(description = "Kilogramos")
    KILOGRAMO
}
