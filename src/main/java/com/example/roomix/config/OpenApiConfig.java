package com.example.roomix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
                        .version("0.0.1-SNAPSHOT")
                        .description("""
                                API REST para la administración hotelera **Roomix**.

                                Módulos disponibles:
                                - **Habitaciones**: estados operativos y reservas a nivel de habitación.
                                - **Inventario**: artículos del hotel (toallas, escobas, routers, almohadas, etc.).

                                Errores de inventario: códigos `INV-xxx` definidos en `InventarioErrorCode`.
                                Formato de fechas: ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss`), zona horaria del servidor.
                                """)
                        .contact(new Contact()
                                .name("Roomix")
                                .email("soporte@roomix.local"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://roomix.local")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Entorno local")
                ));
    }
}
