package com.example.roomix.huesped.dto;

import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "HuespedRequest")
public record HuespedRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80)
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 120)
        String apellidos,

        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 40)
        String numeroDocumento,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 120)
        String email,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30)
        String telefono,

        @Size(max = 80)
        String nacionalidad,

        @Schema(format = "date", nullable = true)
        java.time.LocalDate fechaNacimiento,

        @Size(max = 500)
        String notas,

        Boolean activo
) {
}
