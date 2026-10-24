package com.example.roomix.finanzas.controller;

import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.dto.FacturaDtos.FacturaResponse;
import com.example.roomix.finanzas.dto.FacturaDtos.PagoRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.AnularRequest;
import com.example.roomix.finanzas.service.FacturacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Facturas", description = "Facturación de estadías y eventos, pagos y anulaciones")
@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturacionService facturacionService;

    @Operation(summary = "Listar facturas")
    @GetMapping
    public PaginaResponse<FacturaResponse> listar(
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano
    ) {
        return facturacionService.listar(estado, desde, hasta, texto, pagina, tamano);
    }

    @Operation(summary = "Detalle de factura (para imprimir)")
    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable Long id) {
        return facturacionService.obtener(id);
    }

    @Operation(summary = "Facturas de un huésped")
    @GetMapping("/huesped/{huespedId}")
    public List<FacturaResponse> deHuesped(@PathVariable Long huespedId) {
        return facturacionService.deHuesped(huespedId);
    }

    @Operation(summary = "Facturar estadía", description = "Alojamiento + servicios consumidos no facturados")
    @PostMapping("/reserva/{reservaId}")
    public FacturaResponse facturarReserva(@PathVariable Long reservaId) {
        return facturacionService.facturarReserva(reservaId);
    }

    @Operation(summary = "Facturar evento", description = "Valor base del evento + recursos asignados")
    @PostMapping("/evento/{eventoId}")
    public FacturaResponse facturarEvento(@PathVariable Long eventoId) {
        return facturacionService.facturarEvento(eventoId);
    }

    @Operation(summary = "Registrar pago", description = "Genera automáticamente el ingreso en el módulo financiero")
    @PostMapping("/{id}/pagos")
    public FacturaResponse registrarPago(@PathVariable Long id, @Valid @RequestBody PagoRequest request) {
        return facturacionService.registrarPago(id, request);
    }

    @Operation(summary = "Anular factura", description = "También anula los ingresos de sus pagos")
    @PatchMapping("/{id}/anular")
    public FacturaResponse anular(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return facturacionService.anular(id, request.motivo());
    }
}
