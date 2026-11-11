package com.example.roomix.caja.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CajaErrorCode implements ModuloErrorCode {
    TURNO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "CAJ-001", "No se encontró el turno de caja %s"),
    TURNO_REQUERIDO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-002",
            "Abra su turno de caja (menú Caja, con la base que recibe) antes de registrar pagos, ingresos o egresos"),
    CAJA_OCUPADA(HttpStatus.CONFLICT, "CAJ-003",
            "La caja está abierta por %s desde %s: ese turno debe cerrarse y entregarse antes de abrir uno nuevo"),
    CAJA_DE_OTRO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-004",
            "La caja está a cargo de %s: debe cerrar su turno y entregarle la caja antes de que usted registre pagos o movimientos"),
    TURNO_CERRADO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-005", "El turno de caja %s ya está cerrado"),
    SOLO_CAJERO(HttpStatus.FORBIDDEN, "CAJ-006", "Solo %s (responsable del turno) o un administrador puede hacer esta operación"),
    OBSERVACION_REQUERIDA(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-007", "%s: escriba una observación que lo explique"),
    RETIRO_EXCEDE_EFECTIVO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-008", "El retiro (%s) supera el efectivo en caja (%s)"),
    BASE_EXCEDE_CONTADO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-009", "La base que queda (%s) no puede superar el efectivo contado (%s)"),
    CONTEO_REQUERIDO(HttpStatus.BAD_REQUEST, "CAJ-010", "Indique el conteo de billetes y monedas o el total de efectivo contado"),
    MOVIMIENTO_DE_TURNO_CERRADO(HttpStatus.UNPROCESSABLE_ENTITY, "CAJ-011",
            "El movimiento pertenece al turno de caja %s, que ya se cerró y entregó: registre un movimiento de ajuste en lugar de anularlo");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    CajaErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
