package com.example.roomix.incidencia.dto;

import com.example.roomix.incidencia.domain.AlcanceIncidencia;
import com.example.roomix.incidencia.domain.TipoIncidencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(name = "CrearIncidenciaRequest")
public record CrearIncidenciaRequest(
        @NotNull
        @Schema(description = "Alcance: habitación o zona común", example = "HABITACION")
        AlcanceIncidencia alcance,

        @Schema(description = "ID de habitación (obligatorio si alcance = HABITACION)", nullable = true)
        Long habitacionId,

        @Schema(description = "Ubicación en zona común (obligatorio si alcance = ZONA_COMUN)", nullable = true)
        String ubicacion,

        @NotNull
        @Schema(description = "Tipo de servicio", example = "MANTENIMIENTO")
        TipoIncidencia tipo,

        @Schema(description = "Descripción opcional", nullable = true)
        String descripcion,

        @Schema(description = "Fecha y hora programada (obligatoria para MANTENIMIENTO en habitación)", nullable = true)
        LocalDateTime fechaHoraProgramada
) {
}
