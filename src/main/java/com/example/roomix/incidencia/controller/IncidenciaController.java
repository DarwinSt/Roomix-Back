package com.example.roomix.incidencia.controller;

import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.dto.ActualizarTareaIncidenciaRequest;
import com.example.roomix.incidencia.dto.AsignarIncidenciaRequest;
import com.example.roomix.incidencia.dto.CrearIncidenciaRequest;
import com.example.roomix.incidencia.dto.IncidenciaResponse;
import com.example.roomix.incidencia.service.IncidenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Incidencias", description = "Servicios por habitación: limpieza, mantenimiento, servicio al cuarto")
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    @Operation(summary = "Listar incidencias")
    @GetMapping
    public List<IncidenciaResponse> listar(
            @RequestParam(required = false) EstadoIncidencia estado,
            @RequestParam(required = false) Long habitacionId,
            @RequestParam(required = false) Long personalId,
            @RequestParam(required = false) Boolean activas
    ) {
        return incidenciaService.listar(estado, habitacionId, personalId, activas);
    }

    @Operation(summary = "Obtener incidencia por ID")
    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id) {
        return incidenciaService.obtenerPorId(id);
    }

    @Operation(summary = "Incidencias activas de una habitación")
    @GetMapping("/habitacion/{habitacionId}/activas")
    public List<IncidenciaResponse> activasPorHabitacion(@PathVariable Long habitacionId) {
        return incidenciaService.listarActivasPorHabitacion(habitacionId);
    }

    @Operation(summary = "Crear servicio en habitación (ocupada, libre, reservada o inhabilitada)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse crear(@Valid @RequestBody CrearIncidenciaRequest request) {
        return incidenciaService.crear(request);
    }

    @Operation(summary = "Asignar personal (25% progreso)")
    @PatchMapping("/{id}/asignar")
    public IncidenciaResponse asignar(
            @PathVariable Long id,
            @Valid @RequestBody AsignarIncidenciaRequest request
    ) {
        return incidenciaService.asignar(id, request);
    }

    @Operation(summary = "Iniciar servicio en habitación (50% progreso)")
    @PatchMapping("/{id}/iniciar")
    public IncidenciaResponse iniciar(@PathVariable Long id) {
        return incidenciaService.iniciar(id);
    }

    @Operation(summary = "Actualizar check de tarea")
    @PatchMapping("/{id}/tareas/{tareaId}")
    public IncidenciaResponse actualizarTarea(
            @PathVariable Long id,
            @PathVariable Long tareaId,
            @Valid @RequestBody ActualizarTareaIncidenciaRequest request
    ) {
        return incidenciaService.actualizarTarea(id, tareaId, request);
    }

    @Operation(summary = "Finalizar servicio; habilita habitación si no quedan activas")
    @PatchMapping("/{id}/finalizar")
    public IncidenciaResponse finalizar(@PathVariable Long id) {
        return incidenciaService.finalizar(id);
    }

    @Operation(summary = "Cancelar incidencia")
    @PatchMapping("/{id}/cancelar")
    public IncidenciaResponse cancelar(@PathVariable Long id) {
        return incidenciaService.cancelar(id);
    }
}
