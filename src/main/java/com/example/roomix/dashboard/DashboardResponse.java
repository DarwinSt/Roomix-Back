package com.example.roomix.dashboard;

import com.example.roomix.evento.dto.EventoDtos.EventoResponse;
import com.example.roomix.inventario.dto.ArticuloInventarioResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Panel principal. Cada sección es null si el rol del usuario no tiene acceso a ese módulo.
 */
@Schema(name = "DashboardResponse")
public record DashboardResponse(
        LocalDate fecha,
        String rol,
        Habitaciones habitaciones,
        List<MovimientoHuesped> llegadasHoy,
        List<MovimientoHuesped> salidasHoy,
        Operacion operacion,
        Inventario inventario,
        Finanzas finanzas,
        List<EventoResponse> eventosProximos,
        List<Alerta> alertas
) {

    @Schema(name = "DashboardHabitaciones")
    public record Habitaciones(
            int total,
            int libres,
            int reservadas,
            int ocupadas,
            int enLimpieza,
            int enMantenimiento,
            @Schema(description = "Ocupadas / total") double ocupacionPct
    ) {
    }

    @Schema(name = "DashboardMovimientoHuesped", description = "Llegada o salida prevista (check-in / check-out rápido)")
    public record MovimientoHuesped(
            Long reservaId,
            Long habitacionId,
            String habitacionNumero,
            String huespedNombre,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            int noches,
            @Schema(description = "La fecha prevista ya pasó") boolean atrasada
    ) {
    }

    @Schema(name = "DashboardOperacion")
    public record Operacion(
            long incidenciasSinAsignar,
            long incidenciasActivas,
            long serviciosActivos,
            long personalOcupado
    ) {
    }

    @Schema(name = "DashboardInventario")
    public record Inventario(int articulosStockBajo, List<ArticuloInventarioResponse> criticos) {
    }

    @Schema(name = "DashboardFinanzas")
    public record Finanzas(
            BigDecimal ingresosHoy,
            BigDecimal ingresosSemana,
            BigDecimal ingresosMes,
            BigDecimal egresosMes,
            BigDecimal saldoPorCobrar
    ) {
    }

    @Schema(name = "DashboardAlerta")
    public record Alerta(
            @Schema(description = "INFO, ADVERTENCIA o CRITICA") String nivel,
            String modulo,
            String mensaje,
            @Schema(description = "Ruta del frontend para atender la alerta") String ruta
    ) {
    }
}
