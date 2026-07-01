package com.example.roomix.habitacion.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ciclo operativo de una habitación en el hotel.
 * <p>
 * {@link #INHABILITADO}: la habitación no está operativa; se gestionan servicios
 * (limpieza, mantenimiento, servicio al cuarto, etc.) mediante incidencias.
 */
@Schema(
        name = "EstadoHabitacion",
        description = "Estado operativo de una habitación en el ciclo hotelero"
)
public enum EstadoHabitacion {

    @Schema(description = "Disponible para reservar o asignar")
    LIBRE,

    @Schema(description = "Reserva confirmada; el huésped aún no ha hecho check-in")
    RESERVADO,

    @Schema(description = "Huésped dentro de la habitación; reserva en curso (check-in realizado)")
    OCUPADO,

    @Schema(
            description = """
                    Habitación fuera de servicio operativo. Solo tras check-out o adecuación programada.
                    Mientras dura, se crean incidencias (limpieza, mantenimiento, servicio al cuarto, etc.).
                    """
    )
    INHABILITADO
}
