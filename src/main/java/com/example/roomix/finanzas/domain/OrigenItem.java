package com.example.roomix.finanzas.domain;

/** Origen de cada línea de factura; define la categoría del ingreso cuando se paga. */
public enum OrigenItem {
    ALOJAMIENTO(CategoriaFinanciera.ALOJAMIENTO),
    SERVICIO(CategoriaFinanciera.SERVICIOS),
    EVENTO(CategoriaFinanciera.EVENTOS),
    OTRO(CategoriaFinanciera.OTROS_INGRESOS);

    private final CategoriaFinanciera categoria;

    OrigenItem(CategoriaFinanciera categoria) {
        this.categoria = categoria;
    }

    public CategoriaFinanciera getCategoria() {
        return categoria;
    }
}
