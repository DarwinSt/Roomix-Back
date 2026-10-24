package com.example.roomix.auditoria.config;

import com.example.roomix.auditoria.domain.RegistroAuditoria;
import com.example.roomix.auditoria.service.AuditoriaService;
import com.example.roomix.auth.service.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Set;

/**
 * Registra en auditoría toda operación que modifica datos (POST, PUT, PATCH, DELETE) en la API:
 * usuario, rol, módulo, entidad afectada, resultado HTTP e IP de origen.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS_AUDITADOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    /** Rutas que registran su propio evento (con más detalle) o que no deben guardar datos sensibles. */
    private static final Set<String> RUTAS_EXCLUIDAS = Set.of(
            "/api/auth/login",
            "/api/auth/recuperar",
            "/api/auth/restablecer",
            "/api/auth/cambiar-password"
    );

    private final AuditoriaService auditoriaService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();
        if (!METODOS_AUDITADOS.contains(metodo) || !ruta.startsWith("/api/") || RUTAS_EXCLUIDAS.contains(ruta)) {
            return;
        }

        int status = response.getStatus();
        RutaAuditada partes = analizarRuta(ruta);
        auditoriaService.registrar(RegistroAuditoria.builder()
                .usuario(UsuarioActual.username())
                .rol(UsuarioActual.rol())
                .accion(accion(metodo, partes.accion()))
                .modulo(partes.modulo())
                .entidadId(partes.entidadId())
                .metodoHttp(metodo)
                .ruta(ruta)
                .estadoHttp(status)
                .exitoso(status < 400 && ex == null)
                .ip(AuditoriaService.ipCliente(request))
                .detalle(metodo + " " + ruta + (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                .build());
    }

    /**
     * Separa la ruta en módulo, id de la entidad y acción. Ejemplos:
     * <ul>
     *   <li>/api/habitaciones/5/estado → HABITACIONES, 5, ESTADO</li>
     *   <li>/api/inventario/categorias/3 → INVENTARIO_CATEGORIAS, 3, —</li>
     *   <li>/api/incidencias/7/tareas/12 → INCIDENCIAS, 7, TAREAS</li>
     * </ul>
     * El módulo son los segmentos anteriores al primer número (máximo dos).
     */
    static RutaAuditada analizarRuta(String ruta) {
        String[] segmentos = ruta.replaceFirst("^/api/", "").split("/");
        StringBuilder modulo = new StringBuilder();
        String entidadId = null;
        String accion = null;

        for (int i = 0; i < segmentos.length; i++) {
            String seg = segmentos[i];
            if (seg.isBlank()) {
                continue;
            }
            boolean numerico = esNumero(seg);
            if (entidadId == null && numerico) {
                entidadId = seg;
            } else if (entidadId == null && i < 2) {
                if (!modulo.isEmpty()) {
                    modulo.append('_');
                }
                modulo.append(seg);
            } else if (!numerico) {
                accion = seg;
            }
        }

        return new RutaAuditada(
                normalizar(modulo.isEmpty() ? "api" : modulo.toString()),
                entidadId,
                accion != null ? normalizar(accion) : null
        );
    }

    static String accion(String metodo, String accionRuta) {
        if (accionRuta != null) {
            return accionRuta;
        }
        return switch (metodo) {
            case "POST" -> "CREAR";
            case "PUT" -> "ACTUALIZAR";
            case "PATCH" -> "CAMBIO_ESTADO";
            case "DELETE" -> "ELIMINAR";
            default -> metodo;
        };
    }

    private static boolean esNumero(String valor) {
        return valor.chars().allMatch(Character::isDigit);
    }

    private static String normalizar(String valor) {
        return valor.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    record RutaAuditada(String modulo, String entidadId, String accion) {
    }
}
