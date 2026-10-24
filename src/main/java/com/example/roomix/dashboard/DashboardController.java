package com.example.roomix.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Panel principal según el rol del usuario")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Indicadores del día, llegadas/salidas, alertas y resumen financiero",
            description = "Cada sección se incluye solo si el rol del usuario tiene acceso a ese módulo")
    @GetMapping
    public DashboardResponse obtener() {
        return dashboardService.obtener();
    }
}
