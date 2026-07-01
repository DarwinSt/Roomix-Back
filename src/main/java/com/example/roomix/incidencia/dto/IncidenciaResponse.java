package com.example.roomix.incidencia.dto;

import com.example.roomix.incidencia.domain.AlcanceIncidencia;
import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.domain.Incidencia;
import com.example.roomix.incidencia.domain.IncidenciaProgresoCalculator;
import com.example.roomix.incidencia.domain.TipoIncidencia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "IncidenciaResponse")
@Builder
public record IncidenciaResponse(
        Long id,
        AlcanceIncidencia alcance,
        Long habitacionId,
        String habitacionNumero,
        String ubicacion,
        String ubicacionEtiqueta,
        Long personalAsignadoId,
        String personalAsignadoNombre,
        TipoIncidencia tipo,
        String titulo,
        String descripcion,
        EstadoIncidencia estado,
        int progresoPorcentaje,
        List<IncidenciaTareaResponse> tareas,
        LocalDateTime fechaHoraCreacion,
        LocalDateTime fechaHoraUltimaActualizacion,
        LocalDateTime fechaHoraFinalizacion,
        LocalDateTime fechaHoraProgramada
) {
    public static IncidenciaResponse from(Incidencia incidencia) {
        String habitacionNumero = incidencia.getHabitacion() != null
                ? incidencia.getHabitacion().getNumero()
                : null;
        Long habitacionId = incidencia.getHabitacion() != null
                ? incidencia.getHabitacion().getId()
                : null;

        return IncidenciaResponse.builder()
                .id(incidencia.getId())
                .alcance(incidencia.getAlcance())
                .habitacionId(habitacionId)
                .habitacionNumero(habitacionNumero)
                .ubicacion(incidencia.getUbicacion())
                .ubicacionEtiqueta(etiquetaUbicacion(incidencia))
                .personalAsignadoId(
                        incidencia.getPersonalAsignado() != null ? incidencia.getPersonalAsignado().getId() : null
                )
                .personalAsignadoNombre(
                        incidencia.getPersonalAsignado() != null ? incidencia.getPersonalAsignado().getNombre() : null
                )
                .tipo(incidencia.getTipo())
                .titulo(incidencia.getTitulo())
                .descripcion(incidencia.getDescripcion())
                .estado(incidencia.getEstado())
                .progresoPorcentaje(IncidenciaProgresoCalculator.calcular(
                        incidencia.getEstado(),
                        incidencia.getTareas()
                ))
                .tareas(incidencia.getTareas().stream().map(IncidenciaTareaResponse::from).toList())
                .fechaHoraCreacion(incidencia.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(incidencia.getFechaHoraUltimaActualizacion())
                .fechaHoraFinalizacion(incidencia.getFechaHoraFinalizacion())
                .fechaHoraProgramada(incidencia.getFechaHoraProgramada())
                .build();
    }

    private static String etiquetaUbicacion(Incidencia incidencia) {
        if (incidencia.getAlcance() == AlcanceIncidencia.ZONA_COMUN) {
            return incidencia.getUbicacion() != null ? incidencia.getUbicacion().trim() : "Zona común";
        }
        return incidencia.getHabitacion() != null
                ? "Habitación " + incidencia.getHabitacion().getNumero()
                : "Habitación";
    }
}
