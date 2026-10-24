package com.example.roomix.auth.exception;

import com.example.roomix.common.exception.ModuloErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ModuloErrorCode {
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "AUT-001", "Usuario o contraseña incorrectos"),
    USUARIO_INACTIVO(HttpStatus.FORBIDDEN, "AUT-002", "El usuario está desactivado. Contacte al administrador"),
    USUARIO_BLOQUEADO(HttpStatus.LOCKED, "AUT-003", "Cuenta bloqueada por intentos fallidos. Intente de nuevo después de las %s"),
    PASSWORD_ACTUAL_INCORRECTA(HttpStatus.UNPROCESSABLE_ENTITY, "AUT-004", "La contraseña actual no es correcta"),
    TOKEN_INVALIDO(HttpStatus.UNPROCESSABLE_ENTITY, "AUT-005", "El enlace de recuperación no es válido o ya expiró"),
    USUARIO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "AUT-006", "No se encontró el usuario con id %s"),
    USERNAME_DUPLICADO(HttpStatus.CONFLICT, "AUT-007", "Ya existe un usuario con el nombre de usuario %s"),
    EMAIL_DUPLICADO(HttpStatus.CONFLICT, "AUT-008", "Ya existe un usuario con el correo %s"),
    ULTIMO_ADMIN(HttpStatus.UNPROCESSABLE_ENTITY, "AUT-009", "Debe existir al menos un administrador activo"),
    PASSWORD_DEBIL(HttpStatus.BAD_REQUEST, "AUT-010", "La contraseña debe tener mínimo 8 caracteres e incluir mayúscula, minúscula y número"),
    PASSWORD_REPETIDA(HttpStatus.UNPROCESSABLE_ENTITY, "AUT-011", "La nueva contraseña debe ser diferente a la actual");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageTemplate;

    AuthErrorCode(HttpStatus httpStatus, String code, String messageTemplate) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageTemplate = messageTemplate;
    }
}
