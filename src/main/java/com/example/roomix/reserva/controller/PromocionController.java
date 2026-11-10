package com.example.roomix.reserva.controller;

import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.dto.PromocionDtos.PoliticaReservaRequest;
import com.example.roomix.reserva.dto.PromocionDtos.PoliticaReservaResponse;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionAplicableResponse;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionRequest;
import com.example.roomix.reserva.dto.PromocionDtos.PromocionResponse;
import com.example.roomix.reserva.service.PoliticaReservaService;
import com.example.roomix.reserva.service.PromocionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Promociones y políticas de reserva", description = "Descuentos configurables, abono mínimo y penalidad por cancelación")
@RestController
@RequiredArgsConstructor
public class PromocionController {

    private final PromocionService promocionService;
    private final PoliticaReservaService politicaService;

    @Operation(summary = "Listar promociones")
    @GetMapping("/api/promociones")
    public List<PromocionResponse> listar() {
        return promocionService.listar();
    }

    @Operation(summary = "Promociones que aplican a una reserva, con el descuento calculado")
    @GetMapping("/api/promociones/aplicables")
    public List<PromocionAplicableResponse> aplicables(
            @RequestParam Long habitacionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
            @RequestParam(required = false) CanalReserva canal
    ) {
        return promocionService.aplicables(habitacionId, fechaEntrada, fechaSalida, canal);
    }

    @Operation(summary = "Crear promoción (administrador)")
    @PostMapping("/api/promociones")
    @ResponseStatus(HttpStatus.CREATED)
    public PromocionResponse crear(@Valid @RequestBody PromocionRequest request) {
        return promocionService.crear(request);
    }

    @Operation(summary = "Editar promoción (administrador)")
    @PutMapping("/api/promociones/{id}")
    public PromocionResponse actualizar(@PathVariable Long id, @Valid @RequestBody PromocionRequest request) {
        return promocionService.actualizar(id, request);
    }

    @Operation(summary = "Activar o desactivar promoción (administrador)")
    @PatchMapping("/api/promociones/{id}/activa")
    public PromocionResponse cambiarActiva(@PathVariable Long id, @RequestParam boolean valor) {
        return promocionService.cambiarActiva(id, valor);
    }

    @Operation(summary = "Políticas de reserva: abono mínimo y penalidad por cancelación")
    @GetMapping("/api/politicas-reserva")
    public PoliticaReservaResponse politicas() {
        return politicaService.obtener();
    }

    @Operation(summary = "Actualizar políticas de reserva (administrador)")
    @PutMapping("/api/politicas-reserva")
    public PoliticaReservaResponse actualizarPoliticas(@Valid @RequestBody PoliticaReservaRequest request) {
        return politicaService.actualizar(request);
    }
}
