package com.example.roomix.habitacion.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Por qué la habitación quedó en {@link EstadoHabitacion#INHABILITADO}.
 */
@Schema(description = "Motivo de inhabilitación de la habitación")
public enum MotivoInhabilitacion {

    @Schema(description = "Tras check-out del huésped; servicios antes de quedar LIBRE")
    POST_CHECKOUT,

    @Schema(description = "Adecuación o preparación programada (desde LIBRE o RESERVADO)")
    ADECUACION_PROGRAMADA
}
