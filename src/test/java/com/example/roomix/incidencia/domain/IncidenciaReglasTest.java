package com.example.roomix.incidencia.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas puras de las incidencias: el avance del servicio y qué checklist le toca a cada una. */
class IncidenciaReglasTest {

    private static List<IncidenciaTarea> tareas(int total, int completadas) {
        List<IncidenciaTarea> lista = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            IncidenciaTarea t = new IncidenciaTarea();
            t.setCompletada(i < completadas);
            lista.add(t);
        }
        return lista;
    }

    @Test
    void elAvanceVaDe25AlAsignarA50AlIniciarYSubeConCadaTarea() {
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.CREADA, tareas(4, 0))).isZero();
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.ASIGNADA, tareas(4, 0))).isEqualTo(25);
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.EN_PROGRESO, tareas(4, 0))).isEqualTo(50);
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.EN_PROGRESO, tareas(4, 2))).isEqualTo(75);
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.EN_PROGRESO, List.of())).isEqualTo(50);
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.FINALIZADA, tareas(4, 4))).isEqualTo(100);
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.CANCELADA, tareas(4, 4))).isZero();
    }

    @Test
    void conTodasLasTareasMarcadasQueda99HastaFinalizar() {
        assertThat(IncidenciaProgresoCalculator.calcular(EstadoIncidencia.EN_PROGRESO, tareas(6, 6))).isEqualTo(99);
    }

    @Test
    void cadaTipoDeServicioTieneSuChecklist() {
        assertThat(TipoChecklist.de(TipoIncidencia.LIMPIEZA, ContextoLimpieza.URGENCIA, AlcanceIncidencia.HABITACION))
                .isEqualTo(TipoChecklist.LIMPIEZA_URGENCIA);
        assertThat(TipoChecklist.de(TipoIncidencia.LIMPIEZA, ContextoLimpieza.HUESPED_AUSENTE, AlcanceIncidencia.HABITACION))
                .isEqualTo(TipoChecklist.LIMPIEZA_HUESPED_AUSENTE);
        assertThat(TipoChecklist.de(TipoIncidencia.LIMPIEZA, null, AlcanceIncidencia.HABITACION))
                .isEqualTo(TipoChecklist.LIMPIEZA_POST_CHECKOUT);
        assertThat(TipoChecklist.de(TipoIncidencia.MANTENIMIENTO, null, AlcanceIncidencia.ZONA_COMUN))
                .isEqualTo(TipoChecklist.MANTENIMIENTO_ZONA_COMUN);
        assertThat(TipoChecklist.de(TipoIncidencia.MANTENIMIENTO, null, AlcanceIncidencia.HABITACION))
                .isEqualTo(TipoChecklist.MANTENIMIENTO_HABITACION);
        assertThat(TipoChecklist.de(TipoIncidencia.SERVICIO_CUARTO, null, AlcanceIncidencia.HABITACION))
                .isEqualTo(TipoChecklist.SERVICIO_CUARTO);
        assertThat(TipoChecklist.de(TipoIncidencia.OTRO, null, AlcanceIncidencia.ZONA_COMUN))
                .isEqualTo(TipoChecklist.OTRO_ZONA_COMUN);
    }

    @Test
    void todasLasListasPredeterminadasTienenTareas() {
        for (TipoChecklist t : TipoChecklist.values()) {
            assertThat(t.getPredeterminadas()).as(t.name()).isNotEmpty().doesNotHaveDuplicates();
        }
    }
}
