package com.example.roomix.reserva.dto;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.domain.TarifaTipoHabitacion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "TarifaTipoResponse")
@Builder
public record TarifaTipoResponse(
        TipoHabitacion tipoHabitacion,
        BigDecimal precioNoche,
        LocalDateTime fechaHoraUltimaActualizacion
) {
    public static TarifaTipoResponse from(TarifaTipoHabitacion tarifa) {
        return TarifaTipoResponse.builder()
                .tipoHabitacion(tarifa.getTipoHabitacion())
                .precioNoche(tarifa.getPrecioNoche())
                .fechaHoraUltimaActualizacion(tarifa.getFechaHoraUltimaActualizacion())
                .build();
    }
}
