package com.example.roomix.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Perfiles de usuario del sistema (documento de tesis, "Tipos de usuarios").
 */
@Schema(description = "Rol del usuario; determina los módulos a los que puede acceder")
public enum Rol {
    ADMIN("Administrador general", "Acceso total: configuración, usuarios, auditoría y reportes"),
    RECEPCION("Recepcionista", "Reservas, huéspedes, check-in, check-out y facturación"),
    HOUSEKEEPING("Ama de llaves / Housekeeping", "Estados de habitaciones, limpieza y mantenimiento"),
    FINANZAS("Contador / Finanzas", "Módulo financiero, facturas y reportes"),
    EVENTOS("Coordinador de eventos", "Eventos, salones y catálogo de servicios"),
    ALMACEN("Almacenista", "Inventario, movimientos de stock y proveedores");

    private final String nombre;
    private final String descripcion;

    Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Autoridad de Spring Security asociada (ROLE_ADMIN, ROLE_RECEPCION, ...). */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
