package com.example.roomix.personal.dto;

import com.example.roomix.personal.domain.Personal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "PersonalResponse")
@Builder
public record PersonalResponse(
        Long id,
        String nombre,
        String rol,
        String departamento,
        boolean activo,
        @Schema(description = "Tiene una incidencia asignada o en progreso")
        boolean ocupado
) {
    public static PersonalResponse from(Personal personal, boolean ocupado) {
        return PersonalResponse.builder()
                .id(personal.getId())
                .nombre(personal.getNombre())
                .rol(personal.getRol())
                .departamento(personal.getDepartamento())
                .activo(personal.isActivo())
                .ocupado(ocupado)
                .build();
    }
}
