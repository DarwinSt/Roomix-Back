package com.example.roomix.habitacion.exception;

public class NumeroHabitacionDuplicadoException extends RuntimeException {

    public NumeroHabitacionDuplicadoException(String numero) {
        super("Ya existe una habitación con el número: " + numero);
    }
}
