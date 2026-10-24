package com.example.roomix.auditoria.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaInterceptorTest {

    @Test
    void identificaModuloEntidadYAccion() {
        var r = AuditoriaInterceptor.analizarRuta("/api/habitaciones/5/estado");
        assertThat(r.modulo()).isEqualTo("HABITACIONES");
        assertThat(r.entidadId()).isEqualTo("5");
        assertThat(r.accion()).isEqualTo("ESTADO");
    }

    @Test
    void submoduloSinAccion() {
        var r = AuditoriaInterceptor.analizarRuta("/api/inventario/categorias/3");
        assertThat(r.modulo()).isEqualTo("INVENTARIO_CATEGORIAS");
        assertThat(r.entidadId()).isEqualTo("3");
        assertThat(r.accion()).isNull();
        assertThat(AuditoriaInterceptor.accion("PUT", r.accion())).isEqualTo("ACTUALIZAR");
    }

    @Test
    void rutasAnidadasTomanLaUltimaAccion() {
        var r = AuditoriaInterceptor.analizarRuta("/api/incidencias/7/tareas/12");
        assertThat(r.modulo()).isEqualTo("INCIDENCIAS");
        assertThat(r.entidadId()).isEqualTo("7");
        assertThat(r.accion()).isEqualTo("TAREAS");
    }

    @Test
    void creacionSinId() {
        var r = AuditoriaInterceptor.analizarRuta("/api/reservas");
        assertThat(r.modulo()).isEqualTo("RESERVAS");
        assertThat(r.entidadId()).isNull();
        assertThat(AuditoriaInterceptor.accion("POST", r.accion())).isEqualTo("CREAR");
    }

    @Test
    void guionesSeNormalizan() {
        var r = AuditoriaInterceptor.analizarRuta("/api/reservas/9/check-out");
        assertThat(r.accion()).isEqualTo("CHECK_OUT");
    }
}
