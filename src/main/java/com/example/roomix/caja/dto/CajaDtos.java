package com.example.roomix.caja.dto;

import com.example.roomix.caja.domain.EstadoTurno;
import com.example.roomix.caja.domain.TipoMovimientoCaja;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CajaDtos {

    private CajaDtos() {
    }

    public record AbrirTurnoRequest(
            @NotNull(message = "Indique la base (efectivo) que recibe")
            @DecimalMin(value = "0", message = "La base no puede ser negativa") BigDecimal baseInicial,
            @Size(max = 300) String observacion
    ) {
    }

    public record MovimientoInternoRequest(
            @NotNull(message = "Indique si es retiro o aporte") TipoMovimientoCaja tipo,
            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero") BigDecimal valor,
            @NotBlank(message = "Indique el motivo") @Size(max = 200) String motivo
    ) {
    }

    @Schema(description = "Ingreso o egreso del hotel registrado desde la caja (queda también en Finanzas)")
    public record MovimientoCajaRequest(
            @NotNull(message = "El tipo es obligatorio") TipoMovimiento tipo,
            @NotNull(message = "La categoría es obligatoria") CategoriaFinanciera categoria,
            @NotBlank(message = "El concepto es obligatorio") @Size(max = 200) String concepto,
            @NotNull(message = "El valor es obligatorio")
            @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero") BigDecimal valor,
            @NotNull(message = "Indique el medio de pago") MetodoPago metodoPago,
            @Size(max = 150) String tercero,
            @Size(max = 60) String referencia
    ) {
    }

    public record ConteoItem(
            @NotNull @Min(value = 1, message = "Denominación inválida") Integer denominacion,
            @NotNull @Min(value = 0, message = "La cantidad no puede ser negativa") Integer cantidad
    ) {
    }

    public record CerrarTurnoRequest(
            @Schema(description = "Billetes y monedas contados; si se envía, el total contado se calcula con él")
            @Valid List<ConteoItem> conteo,
            @Schema(description = "Total de efectivo contado (si no se envía el conteo por denominación)")
            @DecimalMin(value = "0", message = "El efectivo contado no puede ser negativo") BigDecimal efectivoContado,
            @NotNull(message = "Indique la base que queda en la caja para el siguiente turno")
            @DecimalMin(value = "0", message = "La base no puede ser negativa") BigDecimal baseSiguiente,
            @Size(max = 120) String recibidoPor,
            @Size(max = 500) String observacion
    ) {
    }

    public record TotalMedio(MetodoPago metodo, String nombre, BigDecimal ingresos, BigDecimal egresos, BigDecimal neto) {
    }

    public record TurnoResponse(
            Long id,
            String cajero,
            String cajeroNombre,
            EstadoTurno estado,
            LocalDateTime fechaHoraApertura,
            BigDecimal baseInicial,
            BigDecimal baseEsperada,
            String observacionApertura,
            LocalDateTime fechaHoraCierre,
            String cerradoPor,
            List<TotalMedio> totalesPorMedio,
            BigDecimal totalIngresos,
            BigDecimal totalEgresos,
            BigDecimal ingresosEfectivo,
            BigDecimal egresosEfectivo,
            BigDecimal retiros,
            BigDecimal aportes,
            @Schema(description = "Abierto: efectivo que debe haber ahora; cerrado: el calculado al cierre")
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            BigDecimal diferencia,
            BigDecimal baseSiguiente,
            BigDecimal valorEntregado,
            String recibidoPor,
            String observacionCierre,
            List<ConteoItem> conteo,
            int cantidadMovimientos
    ) {
    }

    @Schema(description = "Movimiento del turno: de Finanzas (pagos, abonos, ingresos, egresos) o interno de la caja")
    public record MovimientoTurno(
            @Schema(description = "FINANZAS o CAJA") String origen,
            Long id,
            LocalDateTime fechaHora,
            @Schema(description = "INGRESO, EGRESO, RETIRO o APORTE") String tipo,
            String concepto,
            String categoria,
            MetodoPago metodo,
            String metodoNombre,
            BigDecimal valor,
            String referencia,
            Long facturaId,
            String registradoPor,
            boolean anulado
    ) {
    }

    public record DetalleTurno(TurnoResponse turno, List<MovimientoTurno> movimientos) {
    }

    public record EstadoCaja(
            @Schema(description = "Turno abierto (null si la caja está cerrada)") TurnoResponse turnoAbierto,
            @Schema(description = "El turno abierto es del usuario actual") boolean esMio,
            @Schema(description = "Base que dejó el último turno cerrado") BigDecimal baseSugerida,
            String ultimoCajero,
            LocalDateTime ultimoCierre
    ) {
    }
}
