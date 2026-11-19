package com.example.roomix.config;

import java.util.List;

/**
 * Módulos que se licencian por cliente. Los demás (habitaciones, reservas, huéspedes, facturación, finanzas, limpieza,
 * personal, usuarios y auditoría) forman el núcleo y siempre están disponibles.
 */
public enum Modulo {
    CAJA("Caja por turnos", List.of("/api/caja")),
    SERVICIOS("Servicios al huésped", List.of("/api/servicios")),
    EVENTOS("Eventos y salones", List.of("/api/eventos", "/api/salones")),
    INVENTARIO("Inventario", List.of("/api/inventario")),
    REPORTES("Reportes y analítica", List.of("/api/reportes"));

    private final String nombre;
    private final List<String> rutas;

    Modulo(String nombre, List<String> rutas) {
        this.nombre = nombre;
        this.rutas = rutas;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getRutas() {
        return rutas;
    }
}
