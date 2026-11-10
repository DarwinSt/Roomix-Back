package com.example.roomix.finanzas.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FinanzasErrorCode implements ModuloErrorCode {
    MOVIMIENTO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "FIN-001", "No se encontró el movimiento con id %s"),
    CATEGORIA_NO_COINCIDE(HttpStatus.BAD_REQUEST, "FIN-002", "La categoría %s no corresponde a un %s"),
    MOVIMIENTO_YA_ANULADO(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-003", "El movimiento ya está anulado"),
    MOVIMIENTO_AUTOMATICO(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-004", "Este ingreso proviene del pago de una factura: anule el pago desde la factura %s"),
    FACTURA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "FIN-005", "No se encontró la factura con id %s"),
    FACTURA_ANULADA(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-006", "La factura %s está anulada"),
    PAGO_EXCEDE_SALDO(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-007", "El pago (%s) supera el saldo pendiente de la factura (%s)"),
    FACTURA_SIN_ITEMS(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-008", "No hay conceptos para facturar"),
    RESERVA_NO_FACTURABLE(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-009", "Solo se facturan reservas en curso o finalizadas (estado actual: %s)"),
    FACTURA_PAGADA(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-010", "La factura %s ya está pagada en su totalidad"),
    PAGO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "FIN-011", "No se encontró el pago con id %s"),
    ABONO_EXCEDE_TOTAL(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-012", "El abono (%s) supera el saldo pendiente de la reserva (%s)"),
    RESERVA_NO_ABONABLE(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-013", "Solo se registran abonos en reservas confirmadas o en curso (estado actual: %s)"),
    MOVIMIENTO_NO_PENDIENTE(HttpStatus.UNPROCESSABLE_ENTITY, "FIN-014", "El movimiento no está pendiente de pago"),
    GASTO_RECURRENTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "FIN-015", "No se encontró el gasto recurrente con id %s"),
    PENDIENTE_SOLO_EGRESOS(HttpStatus.BAD_REQUEST, "FIN-016", "Solo los egresos pueden quedar pendientes de pago");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    FinanzasErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
