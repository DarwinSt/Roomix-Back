package com.example.roomix.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Rechaza las peticiones a un módulo que no está en la licencia del hotel. */
@Component
@RequiredArgsConstructor
public class LicenciaInterceptor implements HandlerInterceptor {

    private final Licencia licencia;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        for (Modulo m : Modulo.values()) {
            if (!licencia.habilitado(m) && m.getRutas().stream().anyMatch(r -> ruta.equals(r) || ruta.startsWith(r + "/"))) {
                throw new ConfiguracionException(ConfiguracionErrorCode.MODULO_NO_LICENCIADO, m.getNombre());
            }
        }
        return true;
    }
}
