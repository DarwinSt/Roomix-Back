package com.example.roomix.venta.exception;

import com.example.roomix.common.exception.ModuloException;

public class VentaException extends ModuloException {

    public VentaException(VentaErrorCode codigo, Object... args) {
        super("VENTAS", codigo, args);
    }
}
