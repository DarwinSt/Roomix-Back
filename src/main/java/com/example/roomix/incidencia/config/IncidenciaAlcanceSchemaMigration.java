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
public class IncidenciaAlcanceSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS alcance VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS ubicacion VARCHAR(150)");
            jdbcTemplate.execute("ALTER TABLE incidencias ALTER COLUMN habitacion_id DROP NOT NULL");

            jdbcTemplate.update("""
                    UPDATE incidencias
                    SET alcance = 'HABITACION'
                    WHERE alcance IS NULL
                    """);

            jdbcTemplate.execute("""
                    ALTER TABLE incidencias
                    ALTER COLUMN alcance SET NOT NULL
                    """);

            log.info("Migración alcance/ubicación en incidencias aplicada");
        } catch (Exception ex) {
            log.warn("Migración alcance incidencias: {}", ex.getMessage());
        }
    }
}
