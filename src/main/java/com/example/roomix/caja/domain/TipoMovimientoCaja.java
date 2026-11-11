package com.example.roomix.caja.domain;

/**
 * Movimientos internos de la caja: no son ingresos ni egresos del hotel, solo mueven efectivo entre la caja y la
 * administración (por ejemplo, retirar billetes grandes a la caja fuerte o recibir más cambio).
 */
public enum TipoMovimientoCaja {
    RETIRO("Retiro de efectivo"),
    APORTE("Aporte de efectivo");

    private final String nombre;

    TipoMovimientoCaja(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
