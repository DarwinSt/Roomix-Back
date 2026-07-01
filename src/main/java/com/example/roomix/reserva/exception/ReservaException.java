package com.example.roomix.reserva.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReservaException extends RuntimeException {

    private final ReservaErrorCode reservaErrorCode;

    public ReservaException(ReservaErrorCode code) {
        super(code.getMensaje());
        this.reservaErrorCode = code;
    }

    public ReservaException(ReservaErrorCode code, String mensaje) {
        super(mensaje);
        this.reservaErrorCode = code;
    }

    public HttpStatus getHttpStatus() {
        return reservaErrorCode.getHttpStatus();
    }

    public String getErrorCode() {
        return reservaErrorCode.getCodigo();
    }
}
