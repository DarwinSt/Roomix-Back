package com.example.roomix.incidencia.domain;

import java.util.List;

public final class IncidenciaProgresoCalculator {

    private IncidenciaProgresoCalculator() {
    }

  /** 0% creada · 25% asignada · 50% en progreso · 50–99% checks · 100% finalizada */
    public static int calcular(EstadoIncidencia estado, List<IncidenciaTarea> tareas) {
        return switch (estado) {
            case CANCELADA, CREADA -> 0;
            case ASIGNADA -> 25;
            case EN_PROGRESO -> progresoEnProgreso(tareas);
            case FINALIZADA -> 100;
        };
    }

    private static int progresoEnProgreso(List<IncidenciaTarea> tareas) {
        if (tareas == null || tareas.isEmpty()) {
            return 50;
        }
        long completadas = tareas.stream().filter(IncidenciaTarea::isCompletada).count();
        int extra = (int) Math.round(50.0 * completadas / tareas.size());
        return Math.min(99, 50 + extra);
    }
}
