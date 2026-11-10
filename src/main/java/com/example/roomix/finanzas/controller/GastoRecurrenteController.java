package com.example.roomix.finanzas.controller;

import com.example.roomix.finanzas.dto.GastoDtos.GastoRecurrenteRequest;
import com.example.roomix.finanzas.dto.GastoDtos.GastoRecurrenteResponse;
import com.example.roomix.finanzas.service.GastoRecurrenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Tag(name = "Gastos recurrentes", description = "Plantillas mensuales que generan egresos por pagar")
@RestController
@RequestMapping("/api/finanzas/gastos-recurrentes")
@RequiredArgsConstructor
public class GastoRecurrenteController {

    private final GastoRecurrenteService service;

    @Operation(summary = "Listar gastos recurrentes (genera los pendientes del mes si faltan)")
    @GetMapping
    public List<GastoRecurrenteResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Crear gasto recurrente")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GastoRecurrenteResponse crear(@Valid @RequestBody GastoRecurrenteRequest request) {
        return service.crear(request);
    }

    @Operation(summary = "Editar gasto recurrente")
    @PutMapping("/{id}")
    public GastoRecurrenteResponse actualizar(@PathVariable Long id, @Valid @RequestBody GastoRecurrenteRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Activar o desactivar gasto recurrente")
    @PatchMapping("/{id}/activo")
    public GastoRecurrenteResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return service.cambiarActivo(id, valor);
    }
}
