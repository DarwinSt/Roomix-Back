package com.example.roomix.incidencia.exception;

import org.springframework.http.HttpStatus;

public enum IncidenciaErrorCode {

    INCIDENCIA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "INC-001", "Incidencia no encontrada con id: %s"),
    TAREA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "INC-002", "Tarea de incidencia no encontrada con id: %s"),
    HABITACION_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "INC-003", "Habitación no encontrada con id: %s"),
    PERSONAL_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "INC-004", "Personal no encontrado con id: %s"),
    PERSONAL_INACTIVO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-005", "El personal '%s' está inactivo"),
    ESTADO_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-006", "Operación no permitida en estado %s"),
    SIN_PERSONAL_ASIGNADO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-007", "La incidencia debe tener personal asignado"),
    TAREAS_INCOMPLETAS(HttpStatus.UNPROCESSABLE_ENTITY, "INC-008", "Complete todas las tareas antes de finalizar"),
    INCIDENCIA_ACTIVA_EXISTENTE(HttpStatus.CONFLICT, "INC-009", "Ya existe una incidencia activa de tipo %s para la habitación %s"),
    HABITACION_ESTADO_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-010", "No se puede crear el servicio %s en habitación %s"),
    TIPO_NO_PERMITIDO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-011", "El tipo %s no está permitido para habitaciones en estado %s"),
    FECHA_PROGRAMADA_REQUERIDA(HttpStatus.UNPROCESSABLE_ENTITY, "INC-012", "El mantenimiento requiere fecha y hora programada"),
    PERSONAL_OCUPADO(HttpStatus.CONFLICT, "INC-013", "El personal '%s' ya tiene una incidencia activa en curso"),
    HABITACION_REQUERIDA(HttpStatus.UNPROCESSABLE_ENTITY, "INC-014", "Debe indicar la habitación para incidencias de alcance HABITACION"),
    UBICACION_REQUERIDA(HttpStatus.UNPROCESSABLE_ENTITY, "INC-015", "Debe indicar la ubicación (zona común) de la incidencia"),
    ALCANCE_TIPO_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY, "INC-016", "El tipo %s no aplica para incidencias en %s");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    IncidenciaErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String formatMessage(Object... args) {
        return messageTemplate.formatted(args);
    }
}
