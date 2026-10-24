package com.example.roomix.auditoria.controller;

import com.example.roomix.auditoria.service.AuditoriaService;
import com.example.roomix.auditoria.service.AuditoriaService.RegistroAuditoriaResponse;
import com.example.roomix.common.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Auditoría", description = "Registro de acciones realizadas por los usuarios (solo administrador)")
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(summary = "Consultar el registro de auditoría (por defecto, últimos 30 días)")
    @GetMapping
    public PaginaResponse<RegistroAuditoriaResponse> buscar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano
    ) {
        return auditoriaService.buscar(usuario, modulo, accion, desde, hasta, pagina, tamano);
    }

    @Operation(summary = "Módulos con registros de auditoría (para filtros)")
    @GetMapping("/modulos")
    public List<String> modulos() {
        return auditoriaService.modulos();
    }
}
