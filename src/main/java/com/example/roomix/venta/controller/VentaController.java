package com.example.roomix.venta.controller;

import com.example.roomix.venta.dto.VentaDtos.AnularVentaRequest;
import com.example.roomix.venta.dto.VentaDtos.EstadiaActiva;
import com.example.roomix.venta.dto.VentaDtos.ProductoVenta;
import com.example.roomix.venta.dto.VentaDtos.VentaRequest;
import com.example.roomix.venta.dto.VentaDtos.VentaResponse;
import com.example.roomix.venta.service.VentaService;
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
import java.util.List;

@Tag(name = "Ventas de productos", description = "Minibar y tienda: venta de contado o cargo a la habitación, con descuento de inventario")
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService service;

    @Operation(summary = "Productos a la venta (con precio de venta y stock)")
    @GetMapping("/productos")
    public List<ProductoVenta> productos() {
        return service.catalogo();
    }

    @Operation(summary = "Huéspedes alojados a los que se puede cargar la venta")
    @GetMapping("/estadias")
    public List<EstadiaActiva> estadias() {
        return service.estadiasActivas();
    }

    @Operation(summary = "Ventas de una estadía o de un día (por defecto hoy)")
    @GetMapping
    public List<VentaResponse> listar(@RequestParam(required = false) Long reservaId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.listar(reservaId, fecha);
    }

    @Operation(summary = "Una venta (para el ticket)")
    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Registrar una venta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(@Valid @RequestBody VentaRequest request) {
        return service.registrar(request);
    }

    @Operation(summary = "Anular una venta (devuelve los productos al inventario)")
    @PatchMapping("/{id}/anular")
    public VentaResponse anular(@PathVariable Long id, @Valid @RequestBody AnularVentaRequest request) {
        return service.anular(id, request);
    }
}
