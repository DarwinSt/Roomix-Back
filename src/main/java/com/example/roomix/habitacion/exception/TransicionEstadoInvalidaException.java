package com.example.roomix.habitacion.exception;

import com.example.roomix.habitacion.domain.EstadoHabitacion;

public class TransicionEstadoInvalidaException extends RuntimeException {

    public TransicionEstadoInvalidaException(EstadoHabitacion desde, EstadoHabitacion hacia) {
        super("Transición de estado no permitida: %s → %s".formatted(desde, hacia));
    }

    public TransicionEstadoInvalidaException(EstadoHabitacion desde, EstadoHabitacion hacia, String detalle) {
        super("Transición de estado no permitida: %s → %s. %s".formatted(desde, hacia, detalle));
    }
}
