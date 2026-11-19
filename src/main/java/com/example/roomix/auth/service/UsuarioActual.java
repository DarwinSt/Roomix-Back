package com.example.roomix.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Acceso al usuario autenticado de la petición en curso (para auditoría y responsables).
 */
public final class UsuarioActual {

    public static final String SISTEMA = "sistema";

    private UsuarioActual() {
    }

    /** Nombre de usuario autenticado, o "sistema" en procesos sin sesión (arranque, tareas internas). */
    public static String username() {
        return autenticacion().map(Authentication::getName).orElse(SISTEMA);
    }

    /** Rol sin prefijo (ADMIN, RECEPCION, ...) o null si no hay sesión. */
    public static String rol() {
        return autenticacion()
                .flatMap(a -> a.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst())
                .map(r -> r.replaceFirst("^ROLE_", ""))
                .orElse(null);
    }

    /** Roles del usuario (principal y adicionales), sin el prefijo ROLE_. */
    public static java.util.List<String> roles() {
        return autenticacion()
                .map(a -> a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .map(r -> r.replaceFirst("^ROLE_", "")).toList())
                .orElse(java.util.List.of());
    }

    /** true si el usuario tiene alguno de los roles indicados. */
    public static boolean tieneRol(String... roles) {
        java.util.List<String> actuales = roles();
        for (String r : roles) {
            if (actuales.contains(r)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Authentication> autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return Optional.of(auth);
    }
}
