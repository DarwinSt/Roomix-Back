package com.example.roomix.inventario.exception;

import com.example.roomix.common.exception.BusinessException;

public class InventarioException extends BusinessException {

    private final InventarioErrorCode inventarioErrorCode;

    public InventarioException(InventarioErrorCode inventarioErrorCode, Object... args) {
        super(
                inventarioErrorCode.getCode(),
                inventarioErrorCode.getHttpStatus(),
                inventarioErrorCode.formatMessage(args)
        );
        this.inventarioErrorCode = inventarioErrorCode;
    }

    public InventarioErrorCode getInventarioErrorCode() {
        return inventarioErrorCode;
    }
}
