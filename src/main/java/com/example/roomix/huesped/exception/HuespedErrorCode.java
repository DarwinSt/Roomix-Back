package com.example.roomix.huesped.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum HuespedErrorCode {
    HUESPED_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "HUE-001", "No se encontró el huésped con id %s"),
    DOCUMENTO_DUPLICADO(HttpStatus.CONFLICT, "HUE-002", "Ya existe un huésped con el documento %s"),
    HUESPED_INACTIVO(HttpStatus.UNPROCESSABLE_ENTITY, "HUE-003", "El huésped %s está inactivo"),
    HUESPED_REQUERIDO(HttpStatus.UNPROCESSABLE_ENTITY, "HUE-004", "Debe seleccionar un huésped para reservar la habitación"),
    HUESPED_YA_ASIGNADO(HttpStatus.UNPROCESSABLE_ENTITY, "HUE-005", "El huésped ya tiene una reserva o estadía activa en la habitación %s"),
    HUESPED_SIN_ASIGNAR_CHECKIN(HttpStatus.UNPROCESSABLE_ENTITY, "HUE-006", "No hay huésped asignado a esta reserva para hacer check-in");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    HuespedErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String formatMessage(Object... args) {
        return String.format(messageTemplate, args);
    }
}
