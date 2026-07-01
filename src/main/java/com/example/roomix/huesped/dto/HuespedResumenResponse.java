package com.example.roomix.huesped.dto;

import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(name = "HuespedResumenResponse")
@Builder
public record HuespedResumenResponse(
        Long id,
        String nombreCompleto,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String email,
        String telefono
) {
    public static HuespedResumenResponse from(Huesped huesped) {
        if (huesped == null) {
            return null;
        }
        return HuespedResumenResponse.builder()
                .id(huesped.getId())
                .nombreCompleto(huesped.nombreCompleto())
                .tipoDocumento(huesped.getTipoDocumento())
                .numeroDocumento(huesped.getNumeroDocumento())
                .email(huesped.getEmail())
                .telefono(huesped.getTelefono())
                .build();
    }
}
