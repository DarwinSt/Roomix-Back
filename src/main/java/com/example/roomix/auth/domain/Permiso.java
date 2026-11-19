package com.example.roomix.auth.domain;

import com.example.roomix.config.Modulo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permisos por módulo. Cada rol es una plantilla con sus permisos por defecto (que reproducen las reglas de acceso de
 * Roomix 1.1); el administrador puede marcar o quitar permisos a cada usuario. La autoridad es PERM_&lt;nombre&gt;.
 */
public enum Permiso {
    HABITACIONES_VER("Habitaciones", "Ver", "Consultar habitaciones, plano y fotos", null),
    HABITACIONES_OPERAR("Habitaciones", "Operar", "Cambiar estados y datos de la habitación", null),
    HABITACIONES_ADMINISTRAR("Habitaciones", "Administrar", "Crear y eliminar habitaciones y gestionar sus fotos", null),
    RESERVAS_VER("Reservas y huéspedes", "Ver", "Consultar reservas y huéspedes", null),
    RESERVAS_GESTIONAR("Reservas y huéspedes", "Gestionar", "Reservar, check-in, check-out, abonos y registro de huéspedes", null),
    PROMOCIONES_VER("Promociones y políticas", "Ver", "Consultar promociones y políticas de reserva", null),
    PROMOCIONES_GESTIONAR("Promociones y políticas", "Gestionar", "Crear promociones y cambiar las políticas de reserva", null),
    TARIFAS_GESTIONAR("Tarifas", "Gestionar", "Cambiar las tarifas por tipo de habitación", null),
    LIMPIEZA("Limpieza y mantenimiento", "Acceso", "Tareas de limpieza y mantenimiento", null),
    PERSONAL_VER("Personal", "Ver", "Consultar el personal", null),
    PERSONAL_GESTIONAR("Personal", "Gestionar", "Registrar y editar el personal", null),
    INVENTARIO_VER("Inventario", "Ver", "Consultar artículos y movimientos", Modulo.INVENTARIO),
    INVENTARIO_GESTIONAR("Inventario", "Gestionar", "Registrar artículos, entradas, salidas y compras", Modulo.INVENTARIO),
    SERVICIOS_VER("Servicios", "Ver", "Consultar servicios y solicitudes", Modulo.SERVICIOS),
    SERVICIOS_SOLICITUDES("Servicios", "Solicitudes", "Registrar y atender solicitudes de servicio", Modulo.SERVICIOS),
    SERVICIOS_CATALOGO("Servicios", "Catálogo", "Gestionar el catálogo de servicios", Modulo.SERVICIOS),
    EVENTOS_VER("Eventos", "Ver", "Consultar eventos y salones", Modulo.EVENTOS),
    EVENTOS_GESTIONAR("Eventos", "Gestionar", "Crear eventos, salones y participantes", Modulo.EVENTOS),
    FACTURAS("Facturas", "Acceso", "Consultar facturas y registrar pagos", null),
    CAJA_VER("Caja", "Ver", "Consultar turnos de caja", Modulo.CAJA),
    CAJA_OPERAR("Caja", "Operar", "Abrir y cerrar turnos, ingresos, egresos, retiros y aportes", Modulo.CAJA),
    FINANZAS("Finanzas", "Acceso", "Ingresos, egresos, gastos y estado de resultados", null),
    REPORTES("Reportes", "Acceso", "Reportes de ocupación, ADR y RevPAR", Modulo.REPORTES),
    USUARIOS("Usuarios y auditoría", "Acceso", "Crear usuarios, asignar permisos y consultar la auditoría", null),
    CONFIGURACION("Configuración del hotel", "Acceso", "Nombre, logo, impuestos y opciones del hotel", null);

    private final String grupo;
    private final String nivel;
    private final String descripcion;
    private final Modulo modulo;

    Permiso(String grupo, String nivel, String descripcion, Modulo modulo) {
        this.grupo = grupo;
        this.nivel = nivel;
        this.descripcion = descripcion;
        this.modulo = modulo;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getNivel() {
        return nivel;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Módulo de la licencia al que pertenece (null: núcleo, siempre disponible). */
    public Modulo getModulo() {
        return modulo;
    }

    public String autoridad() {
        return "PERM_" + name();
    }

    /** Permisos que marca cada rol (equivalentes a las reglas de acceso de Roomix 1.1). */
    public static Set<Permiso> porDefecto(Rol rol) {
        return switch (rol) {
            case ADMIN -> EnumSet.allOf(Permiso.class);
            case RECEPCION -> EnumSet.of(HABITACIONES_VER, HABITACIONES_OPERAR, RESERVAS_VER, RESERVAS_GESTIONAR, PROMOCIONES_VER,
                    LIMPIEZA, PERSONAL_VER, SERVICIOS_VER, SERVICIOS_SOLICITUDES, EVENTOS_VER, FACTURAS, CAJA_VER, CAJA_OPERAR);
            case HOUSEKEEPING -> EnumSet.of(HABITACIONES_VER, LIMPIEZA, PERSONAL_VER, INVENTARIO_VER, SERVICIOS_VER,
                    SERVICIOS_SOLICITUDES);
            case FINANZAS -> EnumSet.of(RESERVAS_VER, PROMOCIONES_VER, TARIFAS_GESTIONAR, INVENTARIO_VER, SERVICIOS_VER, EVENTOS_VER,
                    FACTURAS, CAJA_VER, FINANZAS, REPORTES);
            case EVENTOS -> EnumSet.of(HABITACIONES_VER, PERSONAL_VER, SERVICIOS_VER, SERVICIOS_SOLICITUDES, SERVICIOS_CATALOGO,
                    EVENTOS_VER, EVENTOS_GESTIONAR);
            case ALMACEN -> EnumSet.of(INVENTARIO_VER, INVENTARIO_GESTIONAR);
        };
    }
}
