package com.example.roomix.servicio.domain;

public enum CategoriaServicio {
    ALIMENTOS_BEBIDAS("Alimentos y bebidas (room service)"),
    LAVANDERIA("Lavandería"),
    SPA_BIENESTAR("Spa y bienestar"),
    TRANSPORTE("Transporte"),
    TOURS("Tours y actividades"),
    OTRO("Otros servicios");

    private final String nombre;

    CategoriaServicio(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
