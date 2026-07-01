package com.example.roomix.habitacion.controller;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.dto.ActualizarEstadoRequest;
import com.example.roomix.habitacion.dto.ErrorResponseDoc;
import com.example.roomix.habitacion.dto.HabitacionRequest;
import com.example.roomix.habitacion.dto.HabitacionResponse;
import com.example.roomix.habitacion.service.HabitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(
        name = "Habitaciones",
        description = """
                Gestión del inventario de habitaciones del hotel: alta, consulta, actualización,
                cambio de estado operativo y baja. Primer módulo del sistema Roomix.
                """
)
@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {

    private final HabitacionService habitacionService;

    @Operation(
            summary = "Listar habitaciones",
            description = "Devuelve todas las habitaciones. Opcionalmente filtra por estado operativo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado obtenido correctamente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = HabitacionResponse.class))
                    )
            )
    })
    @GetMapping
    public List<HabitacionResponse> listar(
            @Parameter(
                    description = "Filtra habitaciones por estado. Si se omite, devuelve todas.",
                    example = "LIBRE"
            )
            @RequestParam(required = false) EstadoHabitacion estado
    ) {
        return habitacionService.listar(estado);
    }

    @Operation(
            summary = "Obtener habitación por ID",
            description = "Consulta el detalle completo de una habitación, incluyendo fechas de reserva y última actualización."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Habitación encontrada",
                    content = @Content(schema = @Schema(implementation = HabitacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una habitación con el ID indicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @GetMapping("/{id}")
    public HabitacionResponse obtener(
            @Parameter(description = "Identificador interno de la habitación", example = "1", required = true)
            @PathVariable Long id
    ) {
        return habitacionService.obtenerPorId(id);
    }

    @Operation(
            summary = "Crear habitación",
            description = """
                    Registra una nueva habitación en el inventario.
                    El estado por defecto es `LIBRE` si no se envía.
                    Si el estado es `RESERVADO`, se puede indicar `fechaHoraReservacion` o se asigna la fecha/hora actual.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Habitación creada",
                    content = @Content(schema = @Schema(implementation = HabitacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una habitación con el mismo número",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabitacionResponse crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de la nueva habitación",
                    required = true,
                    content = @Content(schema = @Schema(implementation = HabitacionRequest.class))
            )
            @Valid @RequestBody HabitacionRequest request
    ) {
        return habitacionService.crear(request);
    }

    @Operation(
            summary = "Actualizar habitación",
            description = """
                    Reemplaza los datos editables de la habitación (número, características, tipo, descripción y opcionalmente estado).
                    La `fechaHoraUltimaActualizacion` se actualiza automáticamente.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Habitación actualizada",
                    content = @Content(schema = @Schema(implementation = HabitacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Habitación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Número de habitación duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Transición de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @PutMapping("/{id}")
    public HabitacionResponse actualizar(
            @Parameter(description = "ID de la habitación a actualizar", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos actualizados de la habitación",
                    required = true,
                    content = @Content(schema = @Schema(implementation = HabitacionRequest.class))
            )
            @Valid @RequestBody HabitacionRequest request
    ) {
        return habitacionService.actualizar(id, request);
    }

    @Operation(
            summary = "Actualizar estado de la habitación",
            description = """
                    Cambia el estado operativo respetando el flujo del hotel:
                    `LIBRE → RESERVADO → OCUPADO → INHABILITADO` (check-out o adecuación programada).
                    En `INHABILITADO` se crean servicios (incidencias) manualmente; la habitación se habilita al completar todos.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado",
                    content = @Content(schema = @Schema(implementation = HabitacionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Habitación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Transición de estado no permitida según el flujo operativo",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @PatchMapping("/{id}/estado")
    public HabitacionResponse actualizarEstado(
            @Parameter(description = "ID de la habitación", example = "1", required = true)
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nuevo estado y, opcionalmente, fecha/hora de reserva",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ActualizarEstadoRequest.class))
            )
            @Valid @RequestBody ActualizarEstadoRequest request
    ) {
        return habitacionService.actualizarEstado(id, request);
    }

    @Operation(
            summary = "Eliminar habitación",
            description = "Elimina permanentemente una habitación del inventario por su ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habitación eliminada"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Habitación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDoc.class))
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @Parameter(description = "ID de la habitación a eliminar", example = "1", required = true)
            @PathVariable Long id
    ) {
        habitacionService.eliminar(id);
    }
}
