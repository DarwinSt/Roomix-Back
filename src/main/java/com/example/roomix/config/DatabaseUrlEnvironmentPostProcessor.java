package com.example.roomix.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convierte {@code DATABASE_URL} (formato {@code postgresql://usuario:clave@host:puerto/db?sslmode=require},
 * el que entregan Railway, Render y Neon) en las propiedades {@code spring.datasource.*}.
 * <p>
 * Solo actúa si no se definió explícitamente {@code DB_URL} ni {@code SPRING_DATASOURCE_URL}.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "roomixDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(databaseUrl)
                || StringUtils.hasText(environment.getProperty("DB_URL"))
                || StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_URL"))) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        if (databaseUrl.startsWith("jdbc:")) {
            props.put("spring.datasource.url", databaseUrl);
        } else {
            URI uri = URI.create(databaseUrl);
            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
            if (uri.getPort() > 0) {
                jdbc.append(':').append(uri.getPort());
            }
            jdbc.append(uri.getRawPath());
            String query = limpiarQuery(uri.getRawQuery());
            if (StringUtils.hasText(query)) {
                jdbc.append('?').append(query);
            }
            props.put("spring.datasource.url", jdbc.toString());

            String userInfo = uri.getRawUserInfo();
            if (StringUtils.hasText(userInfo)) {
                int sep = userInfo.indexOf(':');
                String usuario = sep >= 0 ? userInfo.substring(0, sep) : userInfo;
                props.put("spring.datasource.username", decodificar(usuario));
                if (sep >= 0) {
                    props.put("spring.datasource.password", decodificar(userInfo.substring(sep + 1)));
                }
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    /** El driver JDBC no reconoce {@code channel_binding} (lo añade Neon); se descarta. */
    private static String limpiarQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return Arrays.stream(query.split("&"))
                .filter(param -> !param.startsWith("channel_binding="))
                .collect(Collectors.joining("&"));
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }
}
