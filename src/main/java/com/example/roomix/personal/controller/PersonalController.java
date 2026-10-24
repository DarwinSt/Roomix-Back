package com.example.roomix.personal.controller;

import com.example.roomix.personal.dto.PersonalRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.example.roomix.personal.dto.PersonalResponse;
import com.example.roomix.personal.service.PersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Personal", description = "Personal del hotel asignable a incidencias")
@RestController
@RequestMapping("/api/personal")
@RequiredArgsConstructor
public class PersonalController {

    private final PersonalService personalService;

    @Operation(summary = "Listar personal del hotel")
    @GetMapping
    public List<PersonalResponse> listar(
            @RequestParam(required = false) Boolean activo
    ) {
        return personalService.listar(activo);
    }

    @Operation(summary = "Registrar una persona del personal (solo administrador)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalResponse crear(@Valid @RequestBody PersonalRequest request) {
        return personalService.crear(request);
    }

    @Operation(summary = "Actualizar nombre, cargo o departamento (solo administrador)")
    @PutMapping("/{id}")
    public PersonalResponse actualizar(@PathVariable Long id, @Valid @RequestBody PersonalRequest request) {
        return personalService.actualizar(id, request);
    }

    @Operation(summary = "Activar o desactivar a una persona (no se desactiva si tiene tareas en curso)")
    @PatchMapping("/{id}/activo")
    public PersonalResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return personalService.cambiarActivo(id, valor);
    }
}
