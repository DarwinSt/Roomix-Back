package com.example.roomix.config;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ConfiguracionErrorCode implements ModuloErrorCode {
    MODULO_NO_LICENCIADO(HttpStatus.FORBIDDEN, "CFG-001", "El módulo «%s» no está incluido en la licencia de este hotel"),
    LOGO_INVALIDO(HttpStatus.BAD_REQUEST, "CFG-002", "El logo debe ser una imagen PNG, JPG o WebP de máximo 1 MB"),
    SIN_LOGO(HttpStatus.NOT_FOUND, "CFG-003", "El hotel no tiene logo configurado");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    ConfiguracionErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
