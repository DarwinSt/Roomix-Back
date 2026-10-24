package com.example.roomix.reporte;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "ReporteOcupacionResponse",
        description = "Indicadores hoteleros del periodo: ocupación, ADR (tarifa promedio) y RevPAR (ingreso por habitación disponible)")
public record ReporteOcupacionResponse(
        LocalDate desde,
        LocalDate hasta,
        int habitaciones,
        long nochesDisponibles,
        long nochesVendidas,
        double ocupacionPct,
        @Schema(description = "Average Daily Rate: ingreso de alojamiento / noches vendidas") BigDecimal adr,
        @Schema(description = "Revenue per Available Room: ingreso de alojamiento / noches disponibles") BigDecimal revpar,
        BigDecimal ingresoAlojamiento,
        long reservas,
        long cancelaciones,
        double tasaCancelacionPct,
        double estanciaPromedioNoches,
        List<PuntoDia> serie,
        List<Grupo> porCanal,
        List<Grupo> porTipoHabitacion,
        List<Grupo> porProcedencia,
        List<Grupo> serviciosMasVendidos
) {
    @Schema(name = "PuntoOcupacion")
    public record PuntoDia(LocalDate fecha, long nochesVendidas, double ocupacionPct, BigDecimal ingreso) {
    }

    @Schema(name = "GrupoReporte")
    public record Grupo(String nombre, long cantidad, BigDecimal valor) {
    }
}
