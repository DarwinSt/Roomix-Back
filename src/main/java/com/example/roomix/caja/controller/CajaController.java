package com.example.roomix.caja.controller;

import com.example.roomix.caja.dto.CajaDtos.AbrirTurnoRequest;
import com.example.roomix.caja.dto.CajaDtos.CerrarTurnoRequest;
import com.example.roomix.caja.dto.CajaDtos.DetalleTurno;
import com.example.roomix.caja.dto.CajaDtos.EstadoCaja;
import com.example.roomix.caja.dto.CajaDtos.MovimientoCajaRequest;
import com.example.roomix.caja.dto.CajaDtos.MovimientoInternoRequest;
import com.example.roomix.caja.dto.CajaDtos.TurnoResponse;
import com.example.roomix.caja.service.CajaService;
import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Caja", description = "Turnos de caja de la recepción: apertura con base, movimientos, arqueo y cierre")
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService service;

    @Operation(summary = "Estado de la caja: turno abierto (con totales en vivo) y base que dejó el último turno")
    @GetMapping("/estado")
    public EstadoCaja estado() {
        return service.estado();
    }

    @Operation(summary = "Abrir turno con la base recibida")
    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse abrir(@Valid @RequestBody AbrirTurnoRequest request) {
        return service.abrir(request);
    }

    @Operation(summary = "Registrar un ingreso o egreso del hotel desde la caja (queda también en Finanzas)")
    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrarMovimiento(@Valid @RequestBody MovimientoCajaRequest request) {
        return service.registrarMovimiento(request);
    }

    @Operation(summary = "Registrar un retiro o un aporte de efectivo en el turno abierto")
    @PostMapping("/retiros-aportes")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse registrarInterno(@Valid @RequestBody MovimientoInternoRequest request) {
        return service.registrarInterno(request);
    }

    @Operation(summary = "Arqueo y cierre del turno abierto, con la base que queda y la entrega")
    @PostMapping("/cerrar")
    public TurnoResponse cerrar(@Valid @RequestBody CerrarTurnoRequest request) {
        return service.cerrar(request);
    }

    @Operation(summary = "Historial de turnos (la recepción solo ve los suyos)")
    @GetMapping("/turnos")
    public PaginaResponse<TurnoResponse> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String cajero,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return service.historial(desde, hasta, cajero, pagina, tamano);
    }

    @Operation(summary = "Detalle de un turno con todos sus movimientos")
    @GetMapping("/turnos/{id}")
    public DetalleTurno detalle(@PathVariable Long id) {
        return service.detalle(id);
    }
}
