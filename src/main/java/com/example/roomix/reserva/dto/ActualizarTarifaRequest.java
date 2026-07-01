package com.example.roomix.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(name = "ActualizarTarifaRequest")
public record ActualizarTarifaRequest(
        @NotNull @DecimalMin("0.01") BigDecimal precioNoche
) {
}
