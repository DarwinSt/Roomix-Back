package com.example.roomix.huesped.dto;

import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "HuespedResponse")
@Builder
public record HuespedResponse(
        Long id,
        String nombre,
        String apellidos,
        String nombreCompleto,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String email,
        String telefono,
        String nacionalidad,
        LocalDate fechaNacimiento,
        String notas,
        boolean activo,
        @Schema(description = "Habitación con reserva o estadía activa", nullable = true)
        String habitacionActualNumero,
        LocalDateTime fechaHoraCreacion,
        LocalDateTime fechaHoraUltimaActualizacion
) {
    public static HuespedResponse from(Huesped huesped, String habitacionActualNumero) {
        return HuespedResponse.builder()
                .id(huesped.getId())
                .nombre(huesped.getNombre())
                .apellidos(huesped.getApellidos())
                .nombreCompleto(huesped.nombreCompleto())
                .tipoDocumento(huesped.getTipoDocumento())
                .numeroDocumento(huesped.getNumeroDocumento())
                .email(huesped.getEmail())
                .telefono(huesped.getTelefono())
                .nacionalidad(huesped.getNacionalidad())
                .fechaNacimiento(huesped.getFechaNacimiento())
                .notas(huesped.getNotas())
                .activo(huesped.isActivo())
                .habitacionActualNumero(habitacionActualNumero)
                .fechaHoraCreacion(huesped.getFechaHoraCreacion())
                .fechaHoraUltimaActualizacion(huesped.getFechaHoraUltimaActualizacion())
                .build();
    }
}
