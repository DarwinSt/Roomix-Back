package com.example.roomix.personal.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PersonalErrorCode implements ModuloErrorCode {
    PERSONAL_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "PER-001", "No se encontró el personal con id %s"),
    NOMBRE_DUPLICADO(HttpStatus.CONFLICT, "PER-002", "Ya existe una persona registrada como %s"),
    PERSONAL_OCUPADO(HttpStatus.UNPROCESSABLE_ENTITY, "PER-003",
            "%s tiene tareas asignadas o en curso; reasígnelas antes de desactivarlo");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    PersonalErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
