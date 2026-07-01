package com.example.roomix.incidencia.exception;

import com.example.roomix.common.exception.BusinessException;

public class IncidenciaException extends BusinessException {

    private final IncidenciaErrorCode incidenciaErrorCode;

    public IncidenciaException(IncidenciaErrorCode incidenciaErrorCode, Object... args) {
        super(
                incidenciaErrorCode.getCode(),
                incidenciaErrorCode.getHttpStatus(),
                incidenciaErrorCode.formatMessage(args)
        );
        this.incidenciaErrorCode = incidenciaErrorCode;
    }

    public IncidenciaErrorCode getIncidenciaErrorCode() {
        return incidenciaErrorCode;
    }
}
