package com.example.roomix.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Configuración", description = "Datos del establecimiento")
@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final HotelProperties hotelProperties;

    @Operation(summary = "Datos del hotel para comprobantes y facturas")
    @GetMapping("/hotel")
    public HotelProperties hotel() {
        return hotelProperties;
    }
}
