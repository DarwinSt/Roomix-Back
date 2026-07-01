package com.example.roomix.reserva.dto;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Schema(name = "HabitacionDisponibleResponse")
@Builder
public record HabitacionDisponibleResponse(
        Long habitacionId,
        String numero,
        TipoHabitacion tipoHabitacion,
        String descripcion,
        BigDecimal tarifaNoche,
        int cantidadNoches,
        BigDecimal totalEstimado
) {
}
