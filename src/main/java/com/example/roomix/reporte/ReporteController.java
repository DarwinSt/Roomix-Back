package com.example.roomix.reporte;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Reportes", description = "Indicadores de gestión hotelera")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final com.example.roomix.venta.service.VentaService ventaService;

    private final ReporteService reporteService;

    @io.swagger.v3.oas.annotations.Operation(summary = "Ventas de la tienda y el minibar por producto: unidades, ingresos, costo y margen")
    @GetMapping("/ventas-productos")
    public com.example.roomix.venta.service.VentaService.ReporteVentas ventasProductos(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate desde,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate hasta) {
        return ventaService.reporte(desde, hasta);
    }

    @Operation(summary = "Ocupación, ADR, RevPAR, canales, tipos de habitación y procedencia (por defecto, últimos 30 días)")
    @GetMapping("/ocupacion")
    public ReporteOcupacionResponse ocupacion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return reporteService.ocupacion(desde, hasta);
    }
}
