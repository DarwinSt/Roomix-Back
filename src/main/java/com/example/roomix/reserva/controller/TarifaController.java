package com.example.roomix.reserva.controller;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.dto.ActualizarTarifaRequest;
import com.example.roomix.reserva.dto.TarifaTipoResponse;
import com.example.roomix.reserva.service.TarifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tarifas", description = "Tarifas por tipo de habitación")
@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService tarifaService;

    @Operation(summary = "Listar tarifas por tipo de habitación")
    @GetMapping
    public List<TarifaTipoResponse> listar() {
        return tarifaService.listar();
    }

    @Operation(summary = "Actualizar tarifa de un tipo de habitación")
    @PutMapping("/{tipo}")
    public TarifaTipoResponse actualizar(
            @PathVariable TipoHabitacion tipo,
            @Valid @RequestBody ActualizarTarifaRequest request
    ) {
        return tarifaService.actualizar(tipo, request);
    }
}
