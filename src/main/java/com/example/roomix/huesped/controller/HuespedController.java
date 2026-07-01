package com.example.roomix.huesped.controller;

import com.example.roomix.huesped.dto.HuespedRequest;
import com.example.roomix.huesped.dto.HuespedResponse;
import com.example.roomix.huesped.service.HuespedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Huéspedes", description = "Registro de huéspedes del hotel")
@RestController
@RequestMapping("/api/huespedes")
@RequiredArgsConstructor
public class HuespedController {

    private final HuespedService huespedService;

    @Operation(summary = "Listar huéspedes")
    @GetMapping
    public List<HuespedResponse> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String busqueda
    ) {
        return huespedService.listar(activo, busqueda);
    }

    @Operation(summary = "Obtener huésped por id")
    @GetMapping("/{id}")
    public HuespedResponse obtener(@PathVariable Long id) {
        return huespedService.obtenerPorId(id);
    }

    @Operation(summary = "Registrar huésped")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HuespedResponse crear(@Valid @RequestBody HuespedRequest request) {
        return huespedService.crear(request);
    }

    @Operation(summary = "Actualizar huésped")
    @PutMapping("/{id}")
    public HuespedResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody HuespedRequest request
    ) {
        return huespedService.actualizar(id, request);
    }

    @Operation(summary = "Desactivar huésped")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        huespedService.eliminar(id);
    }
}
