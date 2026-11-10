package com.example.roomix.reserva.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReservaErrorCode {

    RESERVA_NO_ENCONTRADA("RES-001", "Reserva no encontrada", HttpStatus.NOT_FOUND),
    FECHAS_INVALIDAS("RES-002", "La fecha de salida debe ser posterior a la de entrada", HttpStatus.UNPROCESSABLE_ENTITY),
    SOLAPAMIENTO("RES-003", "La habitación ya tiene una reserva en esas fechas", HttpStatus.UNPROCESSABLE_ENTITY),
    HABITACION_NO_LIBRE("RES-004", "La habitación no está disponible para reservar", HttpStatus.UNPROCESSABLE_ENTITY),
    TARIFA_NO_CONFIGURADA("RES-005", "No hay tarifa configurada para este tipo de habitación", HttpStatus.UNPROCESSABLE_ENTITY),
    RESERVA_NO_EDITABLE("RES-006", "La reserva no se puede modificar en su estado actual", HttpStatus.UNPROCESSABLE_ENTITY),
    RESERVA_NO_CANCELABLE("RES-007", "Solo se cancelan reservas confirmadas (antes del check-in)", HttpStatus.UNPROCESSABLE_ENTITY),
    MOTIVO_CANCELACION_REQUERIDO("RES-008", "Indique el motivo de la cancelación", HttpStatus.BAD_REQUEST),
    CHECKIN_NO_PERMITIDO("RES-009", "No se puede hacer check-in de esta reserva", HttpStatus.UNPROCESSABLE_ENTITY),
    CHECKOUT_NO_PERMITIDO("RES-010", "No se puede hacer check-out de esta reserva", HttpStatus.UNPROCESSABLE_ENTITY),
    HUESPED_CON_RESERVA_EN_FECHAS("RES-011", "El huésped ya tiene una reserva activa en esas fechas", HttpStatus.UNPROCESSABLE_ENTITY),
    PROMOCION_NO_ENCONTRADA("RES-012", "Promoción no encontrada", HttpStatus.NOT_FOUND),
    PROMOCION_NO_APLICA("RES-013", "La promoción no aplica a esta reserva", HttpStatus.UNPROCESSABLE_ENTITY),
    PROMOCION_INVALIDA("RES-014", "Los datos de la promoción no son válidos", HttpStatus.BAD_REQUEST),
    ABONO_INSUFICIENTE("RES-015", "El abono no alcanza el mínimo exigido para reservar", HttpStatus.UNPROCESSABLE_ENTITY),
    RESERVA_REQUIERE_PAGO("RES-016", "Las reservas se registran desde el formulario de reserva, con su pago", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String codigo;
    private final String mensaje;
    private final HttpStatus httpStatus;

    ReservaErrorCode(String codigo, String mensaje, HttpStatus httpStatus) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.httpStatus = httpStatus;
    }
}
