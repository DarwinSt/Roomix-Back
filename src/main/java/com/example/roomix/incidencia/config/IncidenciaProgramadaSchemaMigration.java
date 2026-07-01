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
public class IncidenciaProgramadaSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE incidencias ADD COLUMN IF NOT EXISTS fecha_hora_programada TIMESTAMP"
            );
        } catch (Exception ex) {
            log.warn("No se pudo agregar fecha_hora_programada: {}", ex.getMessage());
        }
    }
}
