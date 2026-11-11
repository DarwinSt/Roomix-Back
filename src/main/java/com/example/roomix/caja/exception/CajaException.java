package com.example.roomix.caja.exception;

import com.example.roomix.common.exception.ModuloException;

public class CajaException extends ModuloException {

    public CajaException(CajaErrorCode codigo, Object... args) {
        super("CAJA", codigo, args);
    }
}
