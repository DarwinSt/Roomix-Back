package com.example.roomix.incidencia.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidenciaContextoLimpiezaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS contexto_limpieza VARCHAR(30)");

            jdbcTemplate.update("""
                    UPDATE incidencias
                    SET contexto_limpieza = 'POST_CHECKOUT'
                    WHERE tipo = 'LIMPIEZA'
                      AND contexto_limpieza IS NULL
                      AND habitacion_id IS NOT NULL
                    """);

            log.info("Migración contexto_limpieza en incidencias aplicada");
        } catch (Exception ex) {
            log.warn("Migración contexto limpieza: {}", ex.getMessage());
        }
    }
}
