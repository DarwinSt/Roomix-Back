package com.example.roomix.habitacion.exception;

public class HabitacionNotFoundException extends RuntimeException {

    public HabitacionNotFoundException(Long id) {
        super("Habitación no encontrada con id: " + id);
    }
}
