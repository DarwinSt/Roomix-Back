package com.example.roomix.habitacion.dto;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "HabitacionRequest",
        description = "Cuerpo de solicitud para crear o actualizar una habitación"
)
@Builder
public record HabitacionRequest(
        @Schema(
                description = "Número visible de la habitación en el hotel",
                example = "101",
                maxLength = 20,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "El número de habitación es obligatorio")
        @Size(max = 20, message = "El número no puede superar 20 caracteres")
        String numero,

        @Schema(
                description = "Lista de amenidades o características (WiFi, TV, minibar, etc.)",
                example = "[\"WiFi\", \"TV\", \"Aire acondicionado\", \"Minibar\"]"
        )
        List<@NotBlank(message = "Las características no pueden estar vacías") String> caracteristicas,

        @Schema(
                description = "Clasificación de la habitación según capacidad o categoría",
                example = "DOBLE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "El tipo de habitación es obligatorio")
        TipoHabitacion tipoHabitacion,

        @Schema(
                description = "Descripción comercial o interna de la habitación",
                example = "Habitación doble con vista al jardín",
                maxLength = 500,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @Schema(
                description = "Tarifa por noche de esta habitación",
                example = "120.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "El precio por noche es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio por noche debe ser mayor que cero")
        BigDecimal precioNoche,

        @Schema(
                description = "Estado operativo inicial. Por defecto `LIBRE` si se omite al crear.",
                example = "LIBRE"
        )
        EstadoHabitacion estado,

        @Schema(description = "Fecha planificada de entrada (requerida si estado es RESERVADO)", format = "date", nullable = true)
        LocalDate fechaEntrada,

        @Schema(description = "Fecha planificada de salida (requerida si estado es RESERVADO)", format = "date", nullable = true)
        LocalDate fechaSalida,

        @Schema(description = "Huésped asignado (requerido si estado es RESERVADO)", nullable = true)
        Long huespedId
) {
}
