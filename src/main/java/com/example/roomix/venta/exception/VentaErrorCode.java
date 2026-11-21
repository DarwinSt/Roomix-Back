package com.example.roomix.venta.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum VentaErrorCode implements ModuloErrorCode {
    VENTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, "VTA-001", "No se encontró la venta #%s"),
    PRODUCTO_NO_VENDIBLE(HttpStatus.UNPROCESSABLE_ENTITY, "VTA-002", "«%s» no está a la venta (no tiene precio de venta o está inactivo)"),
    STOCK_INSUFICIENTE(HttpStatus.UNPROCESSABLE_ENTITY, "VTA-003", "No hay suficiente «%s»: disponible %s, solicitado %s"),
    RESERVA_REQUERIDA(HttpStatus.UNPROCESSABLE_ENTITY, "VTA-004", "Para cargar a la habitación indique la estadía (reserva en curso)"),
    RESERVA_NO_EN_CURSO(HttpStatus.UNPROCESSABLE_ENTITY, "VTA-005", "Solo se carga a habitaciones con el huésped alojado (check-in hecho); la reserva #%s está %s"),
    METODO_PAGO_REQUERIDO(HttpStatus.UNPROCESSABLE_ENTITY, "VTA-006", "Indique el medio de pago de la venta"),
    VENTA_YA_ANULADA(HttpStatus.CONFLICT, "VTA-007", "La venta #%s ya está anulada"),
    VENTA_FACTURADA(HttpStatus.CONFLICT, "VTA-008", "La venta #%s ya está en una factura; anule o ajuste la factura"),
    PRODUCTO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "VTA-009", "No se encontró el producto #%s");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    VentaErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
