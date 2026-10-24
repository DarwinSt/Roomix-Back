package com.example.roomix.evento.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EventoErrorCode implements ModuloErrorCode {
    EVENTO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "EVT-001", "No se encontró el evento con id %s"),
    SALON_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "EVT-002", "No se encontró el salón con id %s"),
    SALON_INACTIVO(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-003", "El salón %s no está disponible"),
    FECHAS_INVALIDAS(HttpStatus.BAD_REQUEST, "EVT-004", "La hora de fin debe ser posterior a la de inicio"),
    SALON_OCUPADO(HttpStatus.CONFLICT, "EVT-005", "El salón %s ya tiene un evento en ese horario"),
    CAPACIDAD_SALON(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-006", "La capacidad (%s) supera la del salón %s (%s personas)"),
    CUPO_LLENO(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-007", "El evento alcanzó su capacidad máxima (%s participantes)"),
    TRANSICION_INVALIDA(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-008", "No se puede pasar el evento de %s a %s"),
    EVENTO_CERRADO(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-009", "El evento está %s y no admite cambios"),
    MOTIVO_REQUERIDO(HttpStatus.BAD_REQUEST, "EVT-010", "Indique el motivo de la cancelación"),
    PARTICIPANTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "EVT-011", "No se encontró el participante con id %s"),
    NOMBRE_SALON_DUPLICADO(HttpStatus.CONFLICT, "EVT-012", "Ya existe un salón llamado %s"),
    SOLO_HUESPEDES(HttpStatus.UNPROCESSABLE_ENTITY, "EVT-013", "El evento es exclusivo para huéspedes: seleccione un huésped registrado"),
    PARTICIPANTE_DUPLICADO(HttpStatus.CONFLICT, "EVT-014", "%s ya está inscrito en el evento"),
    NOMBRE_PARTICIPANTE_REQUERIDO(HttpStatus.BAD_REQUEST, "EVT-015", "Indique el nombre del participante o seleccione un huésped");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    EventoErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
