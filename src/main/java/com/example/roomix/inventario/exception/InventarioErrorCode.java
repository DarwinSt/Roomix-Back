package com.example.roomix.inventario.exception;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de errores controlados del módulo de inventario.
 * Cada valor define código HTTP, código de negocio y plantilla del mensaje.
 */
public enum InventarioErrorCode {

    ARTICULO_NO_ENCONTRADO(
            HttpStatus.NOT_FOUND,
            "INV-001",
            "Artículo de inventario no encontrado con id: %s"
    ),
    NOMBRE_DUPLICADO(
            HttpStatus.CONFLICT,
            "INV-002",
            "Ya existe un artículo de inventario con el nombre: %s"
    ),
    ARTICULO_INACTIVO(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INV-003",
            "El artículo '%s' está inactivo y no puede utilizarse"
    ),
    STOCK_INSUFICIENTE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INV-004",
            "Stock insuficiente para '%s'. Disponible: %s, solicitado: %s"
    ),
    CANTIDAD_MINIMA_INVALIDA(
            HttpStatus.BAD_REQUEST,
            "INV-005",
            "La cantidad mínima no puede ser mayor que la cantidad actual (%s)"
    ),
    CATEGORIA_NO_ENCONTRADA(
            HttpStatus.NOT_FOUND,
            "INV-006",
            "Categoría de inventario no encontrada con id: %s"
    ),
    CATEGORIA_NOMBRE_DUPLICADO(
            HttpStatus.CONFLICT,
            "INV-007",
            "Ya existe una categoría con el nombre: %s"
    ),
    CATEGORIA_INACTIVA(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INV-008",
            "La categoría '%s' está inactiva"
    ),
    CATEGORIA_CON_ARTICULOS(
            HttpStatus.CONFLICT,
            "INV-009",
            "No se puede eliminar la categoría '%s' porque tiene %s artículo(s) asociado(s)"
    ),
    CATEGORIA_PREDEFINIDA(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INV-010",
            "La categoría predefinida '%s' no puede eliminarse; desactívela si no la necesita"
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    InventarioErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
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
