package com.example.roomix.config;

import com.example.roomix.auditoria.config.AuditoriaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración MVC. El CORS se define en SecurityConfig (debe aplicarse antes de la autenticación).
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuditoriaInterceptor auditoriaInterceptor;
    private final LicenciaInterceptor licenciaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenciaInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(auditoriaInterceptor).addPathPatterns("/api/**");
    }
}
