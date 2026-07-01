package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Añade estado_retorno y normaliza habitaciones inhabilitadas solo por check-out antiguo.
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class EstadoRetornoSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS estado_retorno VARCHAR(30)"
            );

            int liberadas = jdbcTemplate.update("""
                    UPDATE habitaciones
                    SET estado = 'LIBRE',
                        motivo_inhabilitacion = NULL,
                        estado_retorno = NULL
                    WHERE estado = 'INHABILITADO'
                      AND motivo_inhabilitacion = 'POST_CHECKOUT'
                    """);

            if (liberadas > 0) {
                log.info(
                        "Migración estado_retorno: {} habitación(es) POST_CHECKOUT → LIBRE (limpieza vía incidencia)",
                        liberadas
                );
            }

            log.info("Migración estado_retorno aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración estado_retorno: {}", ex.getMessage(), ex);
        }
    }
}
