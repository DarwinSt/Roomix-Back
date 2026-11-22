package com.example.roomix.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Permisos por defecto de cada rol, incluida la separación de responsabilidades (quien cobra no anula). */
class PermisoTest {

    @Test
    void elAdministradorTieneTodosLosPermisos() {
        assertThat(Permiso.porDefecto(Rol.ADMIN)).isEqualTo(EnumSet.allOf(Permiso.class));
    }

    @Test
    void recepcionCobraYVendePeroNoAnula() {
        assertThat(Permiso.porDefecto(Rol.RECEPCION))
                .contains(Permiso.CAJA_OPERAR, Permiso.VENTAS, Permiso.RESERVAS_GESTIONAR, Permiso.FACTURAS)
                .doesNotContain(Permiso.ANULACIONES, Permiso.FINANZAS, Permiso.USUARIOS, Permiso.CONFIGURACION);
    }

    @Test
    void finanzasRevisaYAnulaPeroNoOperaLaCaja() {
        assertThat(Permiso.porDefecto(Rol.FINANZAS))
                .contains(Permiso.ANULACIONES, Permiso.FINANZAS, Permiso.CAJA_VER)
                .doesNotContain(Permiso.CAJA_OPERAR, Permiso.VENTAS);
    }

    @Test
    void ningunRolQueCobraTraeElPermisoDeAnularSalvoElAdministrador() {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.ADMIN) continue;
            var permisos = Permiso.porDefecto(rol);
            boolean cobra = permisos.contains(Permiso.CAJA_OPERAR) || permisos.contains(Permiso.VENTAS);
            assertThat(cobra && permisos.contains(Permiso.ANULACIONES)).as(rol.name()).isFalse();
        }
    }

    @Test
    void almacenSoloManejaInventario() {
        assertThat(Permiso.porDefecto(Rol.ALMACEN)).containsExactlyInAnyOrder(Permiso.INVENTARIO_VER, Permiso.INVENTARIO_GESTIONAR);
    }

    @Test
    void laAutoridadLlevaElPrefijoPerm() {
        assertThat(Permiso.ANULACIONES.autoridad()).isEqualTo("PERM_ANULACIONES");
    }
}
