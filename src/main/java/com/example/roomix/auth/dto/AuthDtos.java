package com.example.roomix.auth.dto;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * DTOs de autenticación y administración de usuarios.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(name = "LoginRequest")
    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {
    }

    @Schema(name = "LoginResponse")
    public record LoginResponse(
            String token,
            String tipo,
            Instant expiraEn,
            UsuarioResponse usuario
    ) {
    }

    @Schema(name = "UsuarioResponse")
    public record UsuarioResponse(
            Long id,
            String username,
            String nombreCompleto,
            String email,
            Rol rol,
            String rolNombre,
            boolean activo,
            boolean debeCambiarPassword,
            LocalDateTime ultimoAcceso,
            LocalDateTime fechaHoraCreacion
    ) {
        public static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(
                    u.getId(),
                    u.getUsername(),
                    u.getNombreCompleto(),
                    u.getEmail(),
                    u.getRol(),
                    u.getRol().getNombre(),
                    u.isActivo(),
                    u.isDebeCambiarPassword(),
                    u.getUltimoAcceso(),
                    u.getFechaHoraCreacion()
            );
        }
    }

    @Schema(name = "CambiarPasswordRequest")
    public record CambiarPasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
            @NotBlank(message = "La nueva contraseña es obligatoria") String passwordNueva
    ) {
    }

    @Schema(name = "RecuperarPasswordRequest")
    public record RecuperarPasswordRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email
    ) {
    }

    @Schema(name = "RestablecerPasswordRequest")
    public record RestablecerPasswordRequest(
            @NotBlank(message = "El token es obligatorio") String token,
            @NotBlank(message = "La nueva contraseña es obligatoria") String passwordNueva
    ) {
    }

    @Schema(name = "UsuarioRequest")
    public record UsuarioRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio")
            @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo letras, números, punto, guion y guion bajo")
            String username,

            @NotBlank(message = "El nombre completo es obligatorio")
            @Size(max = 120)
            String nombreCompleto,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            @Size(max = 120)
            String email,

            @NotNull(message = "El rol es obligatorio")
            Rol rol,

            @Schema(description = "Obligatoria al crear; opcional al editar (se conserva si va vacía)")
            String password,

            Boolean activo
    ) {
    }

    @Schema(name = "PasswordTemporalResponse")
    public record PasswordTemporalResponse(
            @Schema(description = "Contraseña temporal; el usuario deberá cambiarla al ingresar")
            String passwordTemporal
    ) {
    }

    @Schema(name = "RolResponse")
    public record RolResponse(Rol codigo, String nombre, String descripcion) {
        public static RolResponse from(Rol rol) {
            return new RolResponse(rol, rol.getNombre(), rol.getDescripcion());
        }
    }

    @Schema(name = "MensajeResponse")
    public record MensajeResponse(String mensaje) {
    }
}
