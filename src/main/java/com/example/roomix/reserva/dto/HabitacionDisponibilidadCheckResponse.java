package com.example.roomix.reserva.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Schema(name = "HabitacionDisponibilidadCheckResponse")
@Builder
public record HabitacionDisponibilidadCheckResponse(
        Long habitacionId,
        boolean disponible,
        BigDecimal tarifaNoche,
        int cantidadNoches,
        BigDecimal totalEstimado
) {
}
