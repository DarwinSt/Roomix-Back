package com.example.roomix.config;

/** Cómo se cobra el alojamiento: por habitación y noche, o por persona y noche. */
public enum ModoCobro {
    POR_HABITACION("Por habitación y noche"),
    POR_PERSONA("Por persona y noche");

    private final String nombre;

    ModoCobro(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
