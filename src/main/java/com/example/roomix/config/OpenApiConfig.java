package com.example.roomix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roomixOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Roomix API")
                        .version("1.0.0")
                        .description("""
                                API REST para la administración hotelera **Roomix**.

                                Módulos disponibles:
                                - **Habitaciones**: estados operativos y reservas a nivel de habitación.
                                - **Reservas y tarifas**: estadías, disponibilidad y precio por tipo de habitación.
                                - **Huéspedes**: registro de clientes e historial de reservas.
                                - **Incidencias y personal**: limpieza, mantenimiento y servicios.
                                - **Inventario**: artículos del hotel (toallas, escobas, routers, almohadas, etc.).

                                **Seguridad:** inicie sesión en `POST /api/auth/login` y use el botón *Authorize*
                                con el token. Cada rol (ADMIN, RECEPCION, HOUSEKEEPING, FINANZAS, EVENTOS, ALMACEN)
                                solo accede a sus módulos.

                                Errores: códigos por módulo (`INV-xxx`, `INC-xxx`, `RES-xxx`, ...).
                                Formato de fechas: ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`), zona horaria del negocio (APP_TIMEZONE).
                                """)
                        .contact(new Contact()
                                .name("Roomix")
                                .email("soporte@roomix.local"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://roomix.local")))
                // URL relativa: Swagger UI llama al mismo host donde está desplegado (local o producción)
                // Autenticación: POST /api/auth/login y luego "Authorize" con el token recibido
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtenido en POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Servidor actual")
                ));
    }
}
