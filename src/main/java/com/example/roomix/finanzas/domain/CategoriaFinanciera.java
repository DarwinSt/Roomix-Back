package com.example.roomix.finanzas.domain;

/**
 * Categorías contables simplificadas para ingresos y egresos del hotel.
 */
public enum CategoriaFinanciera {
    // Ingresos
    ALOJAMIENTO(TipoMovimiento.INGRESO, "Alojamiento"),
    SERVICIOS(TipoMovimiento.INGRESO, "Servicios adicionales"),
    EVENTOS(TipoMovimiento.INGRESO, "Eventos"),
    OTROS_INGRESOS(TipoMovimiento.INGRESO, "Otros ingresos"),
    // Egresos
    COMPRAS_INVENTARIO(TipoMovimiento.EGRESO, "Compras de inventario"),
    NOMINA(TipoMovimiento.EGRESO, "Nómina"),
    SERVICIOS_PUBLICOS(TipoMovimiento.EGRESO, "Servicios públicos"),
    MANTENIMIENTO(TipoMovimiento.EGRESO, "Mantenimiento"),
    PROVEEDORES(TipoMovimiento.EGRESO, "Proveedores"),
    ARRIENDO(TipoMovimiento.EGRESO, "Arriendo"),
    IMPUESTOS(TipoMovimiento.EGRESO, "Impuestos"),
    MARKETING(TipoMovimiento.EGRESO, "Publicidad y comisiones"),
    OTROS_EGRESOS(TipoMovimiento.EGRESO, "Otros egresos");

    private final TipoMovimiento tipo;
    private final String nombre;

    CategoriaFinanciera(TipoMovimiento tipo, String nombre) {
        this.tipo = tipo;
        this.nombre = nombre;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public String getNombre() {
        return nombre;
    }
}
