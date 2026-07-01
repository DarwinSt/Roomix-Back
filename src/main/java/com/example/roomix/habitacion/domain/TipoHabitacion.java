package com.example.roomix.habitacion.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "TipoHabitacion",
        description = "Clasificación de la habitación según capacidad o categoría del hotel"
)
public enum TipoHabitacion {

    @Schema(description = "Una cama individual")
    INDIVIDUAL,

    @Schema(description = "Dos camas o cama matrimonial")
    DOBLE,

    @Schema(description = "Tres camas o capacidad para tres personas")
    TRIPLE,

    @Schema(description = "Suite con espacios ampliados o servicios premium")
    SUITE,

    @Schema(description = "Habitación familiar (varias camas o espacio amplio)")
    FAMILIAR,

    @Schema(description = "Categoría ejecutiva o business")
    EJECUTIVA
}
