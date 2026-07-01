package com.example.roomix.habitacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Documentación OpenAPI del formato RFC 7807 ({@code ProblemDetail}) usado en errores.
 */
@Schema(
        name = "ProblemDetail",
        description = "Respuesta de error estándar (RFC 7807)",
        example = """
                {
                  "type": "about:blank",
                  "title": "Habitación no encontrada",
                  "status": 404,
                  "detail": "Habitación no encontrada con id: 99",
                  "timestamp": "2026-05-23T15:30:00Z"
                }
                """
)
public record ErrorResponseDoc(
        @Schema(description = "URI de referencia del tipo de error", example = "about:blank")
        String type,
        @Schema(description = "Título breve del error", example = "Datos inválidos")
        String title,
        @Schema(description = "Código HTTP", example = "400")
        int status,
        @Schema(description = "Detalle legible del error")
        String detail,
        @Schema(description = "Marca de tiempo del error")
        Instant timestamp,
        @Schema(description = "Errores por campo (solo en validación 400)")
        Map<String, String> errores
) {
}
