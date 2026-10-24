package com.example.roomix.finanzas.exception;

import com.example.roomix.common.exception.ModuloException;

public class FinanzasException extends ModuloException {

    public FinanzasException(FinanzasErrorCode codigo, Object... args) {
        super("FINANZAS", codigo, args);
    }
}
