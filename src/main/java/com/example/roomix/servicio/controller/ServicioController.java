package com.example.roomix.servicio.controller;

import com.example.roomix.servicio.domain.CategoriaServicio;
import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.dto.ServicioDtos.AsignarRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CambiarEstadoRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CatalogoRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CatalogoResponse;
import com.example.roomix.servicio.dto.ServicioDtos.SolicitudRequest;
import com.example.roomix.servicio.dto.ServicioDtos.SolicitudResponse;
import com.example.roomix.servicio.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "Servicios", description = "Catálogo de servicios adicionales y solicitudes de huéspedes")
@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final ServicioService servicioService;

    @Operation(summary = "Categorías de servicio")
    @GetMapping("/categorias")
    public List<Map<String, String>> categorias() {
        return Arrays.stream(CategoriaServicio.values())
                .map(c -> Map.of("codigo", c.name(), "nombre", c.getNombre()))
                .toList();
    }

    @Operation(summary = "Listar catálogo de servicios")
    @GetMapping("/catalogo")
    public List<CatalogoResponse> catalogo(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return servicioService.listarCatalogo(soloActivos);
    }

    @Operation(summary = "Crear servicio en el catálogo")
    @PostMapping("/catalogo")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogoResponse crearServicio(@Valid @RequestBody CatalogoRequest request) {
        return servicioService.crearServicio(request);
    }

    @Operation(summary = "Actualizar servicio del catálogo")
    @PutMapping("/catalogo/{id}")
    public CatalogoResponse actualizarServicio(@PathVariable Long id, @Valid @RequestBody CatalogoRequest request) {
        return servicioService.actualizarServicio(id, request);
    }

    @Operation(summary = "Activar o desactivar servicio del catálogo")
    @PatchMapping("/catalogo/{id}/activo")
    public CatalogoResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return servicioService.cambiarActivo(id, valor);
    }

    @Operation(summary = "Listar solicitudes", description = "Filtros: estado, reserva, huésped (historial), fecha (agenda) y activas")
    @GetMapping("/solicitudes")
    public List<SolicitudResponse> solicitudes(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Long reservaId,
            @RequestParam(required = false) Long huespedId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Boolean activas
    ) {
        return servicioService.listarSolicitudes(estado, reservaId, huespedId, fecha, activas);
    }

    @Operation(summary = "Registrar solicitud de servicio", description = "El valor se carga a la cuenta de la reserva")
    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crearSolicitud(@Valid @RequestBody SolicitudRequest request) {
        return servicioService.crearSolicitud(request);
    }

    @Operation(summary = "Asignar responsable y programar la solicitud")
    @PatchMapping("/solicitudes/{id}/asignar")
    public SolicitudResponse asignar(@PathVariable Long id, @RequestBody AsignarRequest request) {
        return servicioService.asignar(id, request);
    }

    @Operation(summary = "Cambiar estado de la solicitud (en proceso, completada, cancelada)")
    @PatchMapping("/solicitudes/{id}/estado")
    public SolicitudResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return servicioService.cambiarEstado(id, request);
    }
}
