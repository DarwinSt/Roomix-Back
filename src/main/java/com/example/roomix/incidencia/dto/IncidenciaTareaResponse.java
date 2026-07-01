package com.example.roomix.incidencia.dto;

import com.example.roomix.incidencia.domain.IncidenciaTarea;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Schema(name = "IncidenciaTareaResponse")
@Builder
public record IncidenciaTareaResponse(
        Long id,
        String descripcion,
        int orden,
        boolean completada,
        LocalDateTime fechaHoraCompletado
) {
    public static IncidenciaTareaResponse from(IncidenciaTarea tarea) {
        return IncidenciaTareaResponse.builder()
                .id(tarea.getId())
                .descripcion(tarea.getDescripcion())
                .orden(tarea.getOrden())
                .completada(tarea.isCompletada())
                .fechaHoraCompletado(tarea.getFechaHoraCompletado())
                .build();
    }
}
