package com.example.roomix.incidencia.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Actualiza el CHECK de tipo en incidencias para incluir SERVICIO_CUARTO y OTRO.
 */
@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class IncidenciaTipoCheckMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE incidencias DROP CONSTRAINT IF EXISTS incidencias_tipo_check");
            jdbcTemplate.execute("ALTER TABLE incidencias DROP CONSTRAINT IF EXISTS chk_incidencia_tipo");

            jdbcTemplate.execute("""
                    ALTER TABLE incidencias
                    ADD CONSTRAINT incidencias_tipo_check CHECK (
                        tipo IN ('LIMPIEZA', 'MANTENIMIENTO', 'SERVICIO_CUARTO', 'OTRO')
                    )
                    """);

            log.info("Migración incidencias_tipo_check aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al actualizar incidencias_tipo_check: {}", ex.getMessage(), ex);
        }
    }
}
