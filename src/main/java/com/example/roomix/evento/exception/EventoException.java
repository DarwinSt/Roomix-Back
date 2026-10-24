package com.example.roomix.evento.exception;

import com.example.roomix.common.exception.ModuloException;

public class EventoException extends ModuloException {

    public EventoException(EventoErrorCode codigo, Object... args) {
        super("EVENTOS", codigo, args);
    }
}
