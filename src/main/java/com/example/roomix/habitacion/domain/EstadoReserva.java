package com.example.roomix.habitacion.domain;

/**
 * Estado de la reserva planificada (independiente del uso real de la habitación).
 */
public enum EstadoReserva {
    /** Reserva confirmada; huésped aún no hace check-in. */
    CONFIRMADA,
    /** Huésped en la habitación (check-in realizado). */
    EN_CURSO,
    /** Estancia finalizada (check-out realizado). */
    FINALIZADA,
    /** Reserva cancelada antes del check-in. */
    CANCELADA
}
