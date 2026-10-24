package com.example.roomix.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Contrato de los enums de errores de los módulos (código, estado HTTP y plantilla de mensaje).
 */
public interface ModuloErrorCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessageTemplate();

    String name();

    default String formatMessage(Object... args) {
        return String.format(getMessageTemplate(), args);
    }
}
