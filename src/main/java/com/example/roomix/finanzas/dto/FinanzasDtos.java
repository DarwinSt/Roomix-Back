package com.example.roomix.finanzas.dto;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FinanzasDtos {

    private FinanzasDtos() {
    }

    @Schema(name = "MovimientoFinancieroRequest")
    public record MovimientoRequest(
            @NotNull(message = "El tipo es obligatorio") TipoMovimiento tipo,
            @NotNull(message = "La categoría es obligatoria") CategoriaFinanciera categoria,
            @NotBlank(message = "El concepto es obligatorio") @Size(max = 200) String concepto,
            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero")
            BigDecimal valor,
            @Schema(description = "Por defecto, hoy") LocalDate fecha,
            MetodoPago metodoPago,
            @Size(max = 150) String tercero,
            @Size(max = 60) String referencia
    ) {
    }

    @Schema(name = "MovimientoFinancieroResponse")
    public record MovimientoResponse(
            Long id,
            TipoMovimiento tipo,
            CategoriaFinanciera categoria,
            String categoriaNombre,
            String concepto,
            BigDecimal valor,
            LocalDate fecha,
            MetodoPago metodoPago,
            String tercero,
            String referencia,
            Long facturaId,
            boolean automatico,
            String registradoPor,
            boolean anulado,
            String motivoAnulacion,
            LocalDateTime fechaHoraCreacion
    ) {
        public static MovimientoResponse from(MovimientoFinanciero m) {
            return new MovimientoResponse(
                    m.getId(), m.getTipo(), m.getCategoria(), m.getCategoria().getNombre(), m.getConcepto(),
                    m.getValor(), m.getFecha(), m.getMetodoPago(), m.getTercero(), m.getReferencia(),
                    m.getFacturaId(), m.getPagoId() != null, m.getRegistradoPor(), m.isAnulado(),
                    m.getMotivoAnulacion(), m.getFechaHoraCreacion()
            );
        }
    }

    @Schema(name = "AnularRequest")
    public record AnularRequest(
            @NotBlank(message = "El motivo es obligatorio") @Size(max = 200) String motivo
    ) {
    }

    @Schema(name = "CategoriaFinancieraResponse")
    public record CategoriaResponse(CategoriaFinanciera codigo, String nombre, TipoMovimiento tipo) {
    }

    @Schema(name = "TotalCategoria")
    public record TotalCategoria(CategoriaFinanciera categoria, String nombre, BigDecimal valor, double porcentaje) {
    }

    @Schema(name = "PuntoSerieFinanciera")
    public record PuntoSerie(String etiqueta, LocalDate desde, BigDecimal ingresos, BigDecimal egresos) {
    }

    @Schema(name = "ComparativoPeriodo")
    public record Comparativo(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal ingresos,
            BigDecimal egresos,
            BigDecimal utilidad,
            Double variacionIngresosPct,
            Double variacionEgresosPct,
            Double variacionUtilidadPct
    ) {
    }

    @Schema(name = "ResumenFinancieroResponse", description = "Estado de resultados simplificado del periodo")
    public record ResumenFinanciero(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal utilidad,
            Double margenPct,
            BigDecimal saldoPorCobrar,
            @Schema(description = "DIA o MES según la longitud del periodo") String granularidad,
            List<TotalCategoria> ingresosPorCategoria,
            List<TotalCategoria> egresosPorCategoria,
            List<PuntoSerie> serie,
            Comparativo periodoAnterior
    ) {
    }
}
