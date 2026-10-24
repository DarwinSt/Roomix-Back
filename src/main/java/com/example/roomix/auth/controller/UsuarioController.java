package com.example.roomix.auth.controller;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.dto.AuthDtos.PasswordTemporalResponse;
import com.example.roomix.auth.dto.AuthDtos.RolResponse;
import com.example.roomix.auth.dto.AuthDtos.UsuarioRequest;
import com.example.roomix.auth.dto.AuthDtos.UsuarioResponse;
import com.example.roomix.auth.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Tag(name = "Usuarios", description = "Gestión de usuarios y roles (solo administrador)")
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar usuarios")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Roles disponibles y su alcance")
    @GetMapping("/roles")
    public List<RolResponse> roles() {
        return Arrays.stream(Rol.values()).map(RolResponse::from).toList();
    }

    @Operation(summary = "Obtener usuario")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @Operation(summary = "Crear usuario", description = "El usuario deberá cambiar la contraseña en su primer ingreso")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @Operation(summary = "Actualizar usuario")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @Operation(summary = "Activar o desactivar usuario")
    @PatchMapping("/{id}/activo")
    public UsuarioResponse cambiarActivo(@PathVariable Long id, @RequestParam boolean valor) {
        return usuarioService.cambiarActivo(id, valor);
    }

    @Operation(summary = "Generar contraseña temporal", description = "Se muestra una sola vez; también desbloquea la cuenta")
    @PostMapping("/{id}/restablecer-password")
    public PasswordTemporalResponse restablecerPassword(@PathVariable Long id) {
        return usuarioService.restablecerPassword(id);
    }
}
