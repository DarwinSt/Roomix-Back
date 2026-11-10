package com.example.roomix.finanzas.dto;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.GastoRecurrente;
import com.example.roomix.finanzas.domain.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class GastoDtos {

    private GastoDtos() {
    }

    @Schema(name = "GastoRecurrenteRequest")
    public record GastoRecurrenteRequest(
            @NotBlank(message = "El concepto es obligatorio") @Size(max = 200) String concepto,
            @NotNull(message = "La categoría es obligatoria") CategoriaFinanciera categoria,
            @NotNull(message = "El valor es obligatorio") @DecimalMin(value = "0.01", message = "El valor debe ser mayor que cero") BigDecimal valor,
            @NotNull(message = "El día de vencimiento es obligatorio") @Min(1) @Max(value = 28, message = "Use un día entre 1 y 28")
            Integer diaVencimiento,
            @Size(max = 150) String tercero,
            MetodoPago metodoPago,
            Boolean activo
    ) {
    }

    @Schema(name = "GastoRecurrenteResponse")
    public record GastoRecurrenteResponse(Long id, String concepto, CategoriaFinanciera categoria, String categoriaNombre,
                                          BigDecimal valor, Integer diaVencimiento, String tercero, MetodoPago metodoPago,
                                          boolean activo, String ultimoPeriodo, LocalDate proximoVencimiento) {
        public static GastoRecurrenteResponse from(GastoRecurrente g, LocalDate proximo) {
            return new GastoRecurrenteResponse(g.getId(), g.getConcepto(), g.getCategoria(), g.getCategoria().getNombre(), g.getValor(),
                    g.getDiaVencimiento(), g.getTercero(), g.getMetodoPago(), g.isActivo(), g.getUltimoPeriodo(), proximo);
        }
    }

    /** Confirmación del pago de un egreso pendiente. */
    @Schema(name = "PagarGastoRequest")
    public record PagarGastoRequest(
            @Schema(description = "Fecha del pago; por defecto, hoy") LocalDate fecha,
            MetodoPago metodoPago,
            @Schema(description = "Valor pagado; por defecto, el valor del gasto") @DecimalMin("0.01") BigDecimal valor,
            @Size(max = 60) String referencia
    ) {
    }
}
