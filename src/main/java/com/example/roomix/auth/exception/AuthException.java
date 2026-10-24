package com.example.roomix.auth.exception;

import com.example.roomix.common.exception.ModuloException;

public class AuthException extends ModuloException {

    public AuthException(AuthErrorCode codigo, Object... args) {
        super("AUTENTICACION", codigo, args);
    }
}
