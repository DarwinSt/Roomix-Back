package com.example.roomix.venta.domain;

/** Cómo se paga una venta de productos. */
public enum ModoVenta {
    /** Se cobra en el momento (efectivo, tarjeta, transferencia): entra a la caja del turno. */
    CONTADO,
    /** Se carga a la cuenta de la habitación y se paga en el check-out con la factura. */
    CARGO_HABITACION
}
