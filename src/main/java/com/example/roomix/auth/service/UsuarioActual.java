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
                .flatMap(a -> a.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(x -> x.startsWith("ROLE_")).findFirst())
                .map(r -> r.replaceFirst("^ROLE_", ""))
                .orElse(null);
    }

    private static java.util.List<String> autoridades(String prefijo) {
        return autenticacion()
                .map(a -> a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(x -> x.startsWith(prefijo)).map(x -> x.substring(prefijo.length())).toList())
                .orElse(java.util.List.of());
    }

    /** true si el rol del usuario es alguno de los indicados. */
    public static boolean tieneRol(String... roles) {
        java.util.List<String> actuales = autoridades("ROLE_");
        for (String r : roles) {
            if (actuales.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /** true si el usuario tiene alguno de los permisos indicados (nombres de {@code Permiso}). */
    public static boolean tienePermiso(String... permisos) {
        java.util.List<String> actuales = autoridades("PERM_");
        for (String p : permisos) {
            if (actuales.contains(p)) {
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
