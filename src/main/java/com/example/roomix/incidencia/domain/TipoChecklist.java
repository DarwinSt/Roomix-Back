package com.example.roomix.incidencia.domain;

import java.util.List;

/**
 * Cada clase de servicio con su propio checklist. El administrador puede cambiar las tareas de cada una; mientras no
 * lo haga se usan las tareas predeterminadas de aquí (las mismas de Roomix 1.1).
 */
public enum TipoChecklist {

    LIMPIEZA_POST_CHECKOUT("Limpieza", "Post check-out", "Al salir el huésped; la habitación no se puede reservar hasta terminar",
            List.of("Preparar materiales de limpieza", "Ingresar a la habitación", "Limpiar baño", "Cambiar ropa de cama",
                    "Limpiar superficies y piso", "Revisión final del servicio")),
    LIMPIEZA_HUESPED_AUSENTE("Limpieza", "Huésped ausente", "Limpieza diaria mientras el huésped está fuera de la habitación",
            List.of("Preparar materiales de limpieza", "Ingresar a la habitación", "Limpiar baño", "Cambiar ropa de cama",
                    "Limpiar superficies y piso", "Revisión final del servicio")),
    LIMPIEZA_URGENCIA("Limpieza", "Urgente (huésped presente)", "Derrames, daños u otra limpieza inmediata con el huésped en la habitación",
            List.of("Preparar materiales de limpieza", "Ingresar a la habitación", "Limpiar baño", "Cambiar ropa de cama",
                    "Limpiar superficies y piso", "Revisión final del servicio")),
    MANTENIMIENTO_HABITACION("Mantenimiento", "En habitación", "Reparaciones y adecuaciones programadas en una habitación",
            List.of("Revisión técnica inicial", "Ejecutar reparación o adecuación", "Verificación de funcionamiento",
                    "Cierre de mantenimiento")),
    MANTENIMIENTO_ZONA_COMUN("Mantenimiento", "En zona común", "Reparaciones en lobby, piscina, salones y demás áreas",
            List.of("Revisión inicial del área", "Ejecutar reparación o adecuación", "Verificación de funcionamiento",
                    "Cierre de mantenimiento")),
    SERVICIO_CUARTO("Servicio al cuarto", "Pedido a la habitación", "Pedidos de comida o bebida entregados en la habitación",
            List.of("Preparar pedido en cocina", "Entregar servicio en habitación", "Retirar vajilla y residuos",
                    "Confirmar servicio con huésped o recepción")),
    OTRO_HABITACION("Otro servicio", "En habitación", "Cualquier otra tarea en una habitación",
            List.of("Iniciar servicio", "Ejecutar tareas del servicio", "Verificación final")),
    OTRO_ZONA_COMUN("Otro servicio", "En zona común", "Cualquier otra tarea en un área común",
            List.of("Iniciar servicio en zona común", "Ejecutar tareas del servicio", "Verificación final"));

    private final String grupo;
    private final String nombre;
    private final String descripcion;
    private final List<String> predeterminadas;

    TipoChecklist(String grupo, String nombre, String descripcion, List<String> predeterminadas) {
        this.grupo = grupo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.predeterminadas = predeterminadas;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getPredeterminadas() {
        return predeterminadas;
    }

    /** Checklist que corresponde a una incidencia según su tipo, el subtipo de limpieza y dónde se hace. */
    public static TipoChecklist de(TipoIncidencia tipo, ContextoLimpieza contexto, AlcanceIncidencia alcance) {
        boolean zonaComun = alcance == AlcanceIncidencia.ZONA_COMUN;
        return switch (tipo) {
            case LIMPIEZA -> contexto == ContextoLimpieza.URGENCIA ? LIMPIEZA_URGENCIA
                    : contexto == ContextoLimpieza.HUESPED_AUSENTE ? LIMPIEZA_HUESPED_AUSENTE : LIMPIEZA_POST_CHECKOUT;
            case MANTENIMIENTO -> zonaComun ? MANTENIMIENTO_ZONA_COMUN : MANTENIMIENTO_HABITACION;
            case SERVICIO_CUARTO -> zonaComun ? OTRO_ZONA_COMUN : SERVICIO_CUARTO;
            case OTRO -> zonaComun ? OTRO_ZONA_COMUN : OTRO_HABITACION;
        };
    }
}
