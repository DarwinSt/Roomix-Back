package com.example.roomix.servicio.exception;

import com.example.roomix.common.exception.ModuloException;

public class ServicioException extends ModuloException {

    public ServicioException(ServicioErrorCode codigo, Object... args) {
        super("SERVICIOS", codigo, args);
    }
}
