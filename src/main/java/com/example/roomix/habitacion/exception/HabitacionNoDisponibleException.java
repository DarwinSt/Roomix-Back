package com.example.roomix.habitacion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class HabitacionNoDisponibleException extends RuntimeException {

    public HabitacionNoDisponibleException(String message) {
        super(message);
    }
}
