package com.example.roomix.finanzas.controller;

import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.dto.FinanzasDtos.AnularRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.CategoriaResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.ResumenFinanciero;
import com.example.roomix.finanzas.service.FinanzasService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "Finanzas", description = "Ingresos, egresos, flujo de caja y estado de resultados")
@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {

    private final FinanzasService finanzasService;

    @Operation(summary = "Resumen financiero del periodo",
            description = "Ingresos, egresos, utilidad, desglose por categoría, serie para gráficos y comparativo con el periodo anterior")
    @GetMapping("/resumen")
    public ResumenFinanciero resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return finanzasService.resumen(desde, hasta);
    }

    @Operation(summary = "Listar movimientos (ingresos y egresos)")
    @GetMapping("/movimientos")
    public PaginaResponse<MovimientoResponse> movimientos(
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) CategoriaFinanciera categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "false") boolean incluirAnulados,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano
    ) {
        return finanzasService.listar(tipo, categoria, desde, hasta, incluirAnulados, texto, pagina, tamano);
    }

    @Operation(summary = "Registrar ingreso o egreso manual")
    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@Valid @RequestBody MovimientoRequest request) {
        return finanzasService.registrarManual(request);
    }

    @Operation(summary = "Anular movimiento (con motivo)")
    @PatchMapping("/movimientos/{id}/anular")
    public MovimientoResponse anular(@PathVariable Long id, @Valid @RequestBody AnularRequest request) {
        return finanzasService.anular(id, request.motivo());
    }

    @Operation(summary = "Categorías financieras")
    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return finanzasService.categorias();
    }

    @Operation(summary = "Métodos de pago")
    @GetMapping("/metodos-pago")
    public List<Map<String, String>> metodosPago() {
        return Arrays.stream(MetodoPago.values())
                .map(m -> Map.of("codigo", m.name(), "nombre", m.getNombre()))
                .toList();
    }
}
