package com.example.roomix.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class ConfiguracionDtos {

    private ConfiguracionDtos() {
    }

    public record ModuloInfo(Modulo codigo, String nombre, boolean habilitado) {
    }

    @Schema(description = "Datos públicos (pantalla de ingreso, menú): nombre, logo, módulos y opciones de la interfaz")
    public record ConfiguracionPublica(
            String nombre,
            boolean tieneLogo,
            @Schema(description = "Cambia cada vez que se actualiza el logo (para refrescar la imagen)") long logoVersion,
            List<Modulo> modulos,
            ModoCobro modoCobro,
            boolean cobraIva,
            @Schema(description = "Color principal (#RRGGBB); null = colores de Roomix") String colorPrimario,
            @Schema(description = "Color de acento (#RRGGBB); null = colores de Roomix") String colorAcento,
            String eslogan
    ) {
    }

    public record ConfiguracionResponse(
            String nombre,
            String nit,
            String direccion,
            String ciudad,
            String telefono,
            String email,
            String prefijoFactura,
            boolean cobraIva,
            BigDecimal ivaPorcentaje,
            boolean exentoIvaExtranjeros,
            ModoCobro modoCobro,
            int mesesRetencionHuespedes,
            boolean correoConfirmacionReserva,
            @Schema(description = "El servidor tiene configurado el envío de correos (Brevo o SMTP)") boolean correoDisponible,
            boolean tieneLogo,
            long logoVersion,
            String clienteLicencia,
            List<ModuloInfo> modulos,
            String colorPrimario,
            String colorAcento,
            String eslogan
    ) {
    }

    public record ConfiguracionRequest(
            @NotBlank(message = "El nombre del hotel es obligatorio") @Size(max = 120) String nombre,
            @Size(max = 40) String nit,
            @Size(max = 150) String direccion,
            @Size(max = 100) String ciudad,
            @Size(max = 40) String telefono,
            @Email(message = "El correo no es válido") @Size(max = 150) String email,
            @Size(max = 10) String prefijoFactura,
            @NotNull(message = "Indique si el hotel cobra IVA") Boolean cobraIva,
            @DecimalMin("0") @DecimalMax(value = "100", message = "El IVA no puede superar 100 %") BigDecimal ivaPorcentaje,
            Boolean exentoIvaExtranjeros,
            @NotNull(message = "Indique el modo de cobro") ModoCobro modoCobro,
            @Min(value = 0, message = "Los meses no pueden ser negativos") @Max(120) Integer mesesRetencionHuespedes,
            Boolean correoConfirmacionReserva
    ) {
    }

    @Schema(description = "Apariencia del hotel: colores de la interfaz y frase de bienvenida")
    public record AparienciaRequest(
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color principal debe tener el formato #RRGGBB") String colorPrimario,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "El color de acento debe tener el formato #RRGGBB") String colorAcento,
            @Size(max = 120, message = "La frase admite máximo 120 caracteres") String eslogan
    ) {
    }
}
