package com.example.roomix.auth.controller;

import com.example.roomix.auth.dto.AuthDtos.CambiarPasswordRequest;
import com.example.roomix.auth.dto.AuthDtos.LoginRequest;
import com.example.roomix.auth.dto.AuthDtos.LoginResponse;
import com.example.roomix.auth.dto.AuthDtos.MensajeResponse;
import com.example.roomix.auth.dto.AuthDtos.RecuperarPasswordRequest;
import com.example.roomix.auth.dto.AuthDtos.RestablecerPasswordRequest;
import com.example.roomix.auth.dto.AuthDtos.UsuarioResponse;
import com.example.roomix.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Inicio de sesión, perfil y recuperación de contraseña")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Iniciar sesión", description = "Devuelve un JWT para enviar en el encabezado Authorization: Bearer <token>")
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http);
    }

    @Operation(summary = "Perfil del usuario autenticado")
    @GetMapping("/me")
    public UsuarioResponse perfil() {
        return authService.perfil();
    }

    @Operation(summary = "Cambiar la contraseña propia")
    @PostMapping("/cambiar-password")
    public MensajeResponse cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request, HttpServletRequest http) {
        authService.cambiarPassword(request, http);
        return new MensajeResponse("Contraseña actualizada correctamente");
    }

    @Operation(summary = "Solicitar enlace de recuperación de contraseña por correo")
    @SecurityRequirements
    @PostMapping("/recuperar")
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarPasswordRequest request, HttpServletRequest http) {
        boolean correoActivo = authService.solicitarRecuperacion(request.email(), http);
        return new MensajeResponse(correoActivo
                ? "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."
                : "El envío de correos no está habilitado. Solicita al administrador una contraseña temporal.");
    }

    @Operation(summary = "Restablecer la contraseña con el enlace de recuperación")
    @SecurityRequirements
    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerPasswordRequest request, HttpServletRequest http) {
        authService.restablecer(request, http);
        return new MensajeResponse("Contraseña restablecida. Ya puedes iniciar sesión.");
    }
}
