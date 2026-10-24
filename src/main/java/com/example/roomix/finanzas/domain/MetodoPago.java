package com.example.roomix.finanzas.domain;

public enum MetodoPago {
    EFECTIVO("Efectivo"),
    TARJETA_CREDITO("Tarjeta de crédito"),
    TARJETA_DEBITO("Tarjeta débito"),
    TRANSFERENCIA("Transferencia bancaria"),
    PAGO_DIGITAL("Billetera digital (Nequi, Daviplata)"),
    OTRO("Otro");

    private final String nombre;

    MetodoPago(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
