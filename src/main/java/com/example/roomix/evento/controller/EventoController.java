package com.example.roomix.evento.controller;

import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoAcceso;
import com.example.roomix.evento.domain.EventoEnums.TipoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoRecurso;
import com.example.roomix.evento.dto.EventoDtos.CambiarEstadoRequest;
import com.example.roomix.evento.dto.EventoDtos.EventoRequest;
import com.example.roomix.evento.dto.EventoDtos.EventoResponse;
import com.example.roomix.evento.dto.EventoDtos.ParticipanteRequest;
import com.example.roomix.evento.dto.EventoDtos.ReporteEventos;
import com.example.roomix.evento.dto.EventoDtos.SalonRequest;
import com.example.roomix.evento.dto.EventoDtos.SalonResponse;
import com.example.roomix.evento.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Eventos", description = "Eventos en las instalaciones del hotel, salones, participantes y recursos")
@RestController
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    // ---------------------------------------------------------------- Salones

    @Operation(summary = "Listar salones")
    @GetMapping("/api/salones")
    public List<SalonResponse> salones() {
        return eventoService.listarSalones();
    }

    @Operation(summary = "Crear salón")
    @PostMapping("/api/salones")
    @ResponseStatus(HttpStatus.CREATED)
    public SalonResponse crearSalon(@Valid @RequestBody SalonRequest request) {
        return eventoService.guardarSalon(null, request);
    }

    @Operation(summary = "Actualizar salón")
    @PutMapping("/api/salones/{id}")
    public SalonResponse actualizarSalon(@PathVariable Long id, @Valid @RequestBody SalonRequest request) {
        return eventoService.guardarSalon(id, request);
    }

    // ---------------------------------------------------------------- Eventos

    @Operation(summary = "Catálogos del módulo (tipos, accesos, recursos)")
    @GetMapping("/api/eventos/catalogos")
    public Map<String, List<Map<String, String>>> catalogos() {
        Map<String, List<Map<String, String>>> mapa = new LinkedHashMap<>();
        mapa.put("tipos", Arrays.stream(TipoEvento.values()).map(t -> Map.of("codigo", t.name(), "nombre", t.getNombre())).toList());
        mapa.put("accesos", Arrays.stream(TipoAcceso.values()).map(t -> Map.of("codigo", t.name(), "nombre", t.getNombre())).toList());
        mapa.put("recursos", Arrays.stream(TipoRecurso.values()).map(t -> Map.of("codigo", t.name(), "nombre", t.getNombre())).toList());
        return mapa;
    }

    @Operation(summary = "Listar eventos", description = "Por defecto: desde hace 30 días hasta dentro de 90 días")
    @GetMapping("/api/eventos")
    public List<EventoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) EstadoEvento estado
    ) {
        return eventoService.listar(desde, hasta, estado);
    }

    @Operation(summary = "Reporte de eventos del periodo")
    @GetMapping("/api/eventos/reporte")
    public ReporteEventos reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return eventoService.reporte(desde, hasta);
    }

    @Operation(summary = "Detalle del evento")
    @GetMapping("/api/eventos/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return eventoService.obtener(id);
    }

    @Operation(summary = "Crear evento", description = "Valida capacidad del salón y que no haya otro evento en el mismo horario")
    @PostMapping("/api/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse crear(@Valid @RequestBody EventoRequest request) {
        return eventoService.crear(request);
    }

    @Operation(summary = "Actualizar evento")
    @PutMapping("/api/eventos/{id}")
    public EventoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EventoRequest request) {
        return eventoService.actualizar(id, request);
    }

    @Operation(summary = "Cambiar estado (programado → en curso → finalizado, o cancelado con motivo)")
    @PatchMapping("/api/eventos/{id}/estado")
    public EventoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return eventoService.cambiarEstado(id, request);
    }

    @Operation(summary = "Inscribir participante")
    @PostMapping("/api/eventos/{id}/participantes")
    public EventoResponse agregarParticipante(@PathVariable Long id, @Valid @RequestBody ParticipanteRequest request) {
        return eventoService.agregarParticipante(id, request);
    }

    @Operation(summary = "Marcar confirmación o asistencia de un participante")
    @PatchMapping("/api/eventos/{id}/participantes/{participanteId}")
    public EventoResponse actualizarParticipante(
            @PathVariable Long id,
            @PathVariable Long participanteId,
            @RequestParam(required = false) Boolean confirmado,
            @RequestParam(required = false) Boolean asistio
    ) {
        return eventoService.actualizarParticipante(id, participanteId, confirmado, asistio);
    }

    @Operation(summary = "Retirar participante")
    @DeleteMapping("/api/eventos/{id}/participantes/{participanteId}")
    public EventoResponse eliminarParticipante(@PathVariable Long id, @PathVariable Long participanteId) {
        return eventoService.eliminarParticipante(id, participanteId);
    }
}
