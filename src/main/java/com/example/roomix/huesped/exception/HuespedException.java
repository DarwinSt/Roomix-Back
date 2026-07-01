package com.example.roomix.huesped.exception;

import com.example.roomix.common.exception.BusinessException;

public class HuespedException extends BusinessException {

    private final HuespedErrorCode huespedErrorCode;

    public HuespedException(HuespedErrorCode huespedErrorCode, Object... args) {
        super(
                huespedErrorCode.getCode(),
                huespedErrorCode.getHttpStatus(),
                huespedErrorCode.formatMessage(args)
        );
        this.huespedErrorCode = huespedErrorCode;
    }

    public HuespedErrorCode getHuespedErrorCode() {
        return huespedErrorCode;
    }
}
