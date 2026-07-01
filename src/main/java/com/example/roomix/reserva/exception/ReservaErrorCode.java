package com.example.roomix.reserva.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReservaErrorCode {

    RESERVA_NO_ENCONTRADA("RES-001", "Reserva no encontrada", HttpStatus.NOT_FOUND),
    FECHAS_INVALIDAS("RES-002", "La fecha de salida debe ser posterior a la de entrada", HttpStatus.UNPROCESSABLE_ENTITY),
    SOLAPAMIENTO("RES-003", "La habitación ya tiene una reserva en esas fechas", HttpStatus.UNPROCESSABLE_ENTITY),
    HABITACION_NO_LIBRE("RES-004", "La habitación no está disponible para reservar", HttpStatus.UNPROCESSABLE_ENTITY),
    TARIFA_NO_CONFIGURADA("RES-005", "No hay tarifa configurada para este tipo de habitación", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String codigo;
    private final String mensaje;
    private final HttpStatus httpStatus;

    ReservaErrorCode(String codigo, String mensaje, HttpStatus httpStatus) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.httpStatus = httpStatus;
    }
}
