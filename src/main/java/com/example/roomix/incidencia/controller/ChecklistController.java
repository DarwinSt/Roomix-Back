package com.example.roomix.incidencia.controller;

import com.example.roomix.incidencia.domain.TipoChecklist;
import com.example.roomix.incidencia.dto.ChecklistDtos.ChecklistRequest;
import com.example.roomix.incidencia.dto.ChecklistDtos.ChecklistResponse;
import com.example.roomix.incidencia.service.ChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Checklists de servicio", description = "Acciones de aseo y mantenimiento que define el administrador")
@RestController
@RequestMapping("/api/incidencias/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService service;

    @Operation(summary = "Checklists de cada tipo de servicio")
    @GetMapping
    public List<ChecklistResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Cambiar las tareas de un checklist (aplica a los servicios que se creen después)")
    @PutMapping("/{tipo}")
    public ChecklistResponse actualizar(@PathVariable TipoChecklist tipo, @Valid @RequestBody ChecklistRequest request) {
        return service.actualizar(tipo, request);
    }

    @Operation(summary = "Volver a las tareas predeterminadas")
    @DeleteMapping("/{tipo}")
    public ChecklistResponse restaurar(@PathVariable TipoChecklist tipo) {
        return service.restaurar(tipo);
    }
}
