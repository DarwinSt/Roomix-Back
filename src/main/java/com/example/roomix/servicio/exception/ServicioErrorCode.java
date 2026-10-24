package com.example.roomix.servicio.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ServicioErrorCode implements ModuloErrorCode {
    SERVICIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "SRV-001", "No se encontró el servicio con id %s"),
    NOMBRE_DUPLICADO(HttpStatus.CONFLICT, "SRV-002", "Ya existe un servicio llamado %s"),
    SERVICIO_INACTIVO(HttpStatus.UNPROCESSABLE_ENTITY, "SRV-003", "El servicio %s no está disponible"),
    SOLICITUD_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "SRV-004", "No se encontró la solicitud con id %s"),
    RESERVA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "SRV-005", "No se encontró la reserva con id %s"),
    RESERVA_NO_ACTIVA(HttpStatus.UNPROCESSABLE_ENTITY, "SRV-006", "Solo se pueden solicitar servicios para reservas confirmadas o en curso"),
    TRANSICION_INVALIDA(HttpStatus.UNPROCESSABLE_ENTITY, "SRV-007", "No se puede pasar la solicitud de %s a %s"),
    MOTIVO_REQUERIDO(HttpStatus.BAD_REQUEST, "SRV-008", "Indique el motivo de la cancelación"),
    SOLICITUD_FACTURADA(HttpStatus.UNPROCESSABLE_ENTITY, "SRV-009", "La solicitud ya fue facturada y no se puede cancelar");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    ServicioErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
