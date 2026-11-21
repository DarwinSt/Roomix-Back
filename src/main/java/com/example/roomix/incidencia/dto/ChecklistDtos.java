package com.example.roomix.incidencia.dto;

import com.example.roomix.incidencia.domain.TipoChecklist;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChecklistDtos {

    private ChecklistDtos() {
    }

    @Schema(description = "Checklist de un tipo de servicio (limpieza, mantenimiento…)")
    public record ChecklistResponse(
            TipoChecklist tipo,
            String grupo,
            String nombre,
            String descripcion,
            List<String> tareas,
            @Schema(description = "true si el administrador cambió las tareas predeterminadas") boolean personalizada,
            List<String> predeterminadas
    ) {
    }

    @Schema(description = "Tareas del checklist, en el orden en que se hacen")
    public record ChecklistRequest(
            @NotEmpty(message = "El checklist debe tener al menos una tarea")
            @Size(max = 20, message = "Máximo 20 tareas por checklist")
            List<@NotBlank(message = "Las tareas no pueden estar vacías")
                 @Size(max = 150, message = "Cada tarea admite máximo 150 caracteres") String> tareas
    ) {
    }
}
