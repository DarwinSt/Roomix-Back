package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migra PENDIENTE_LIMPIEZA → INHABILITADO, motivo_pendiente_limpieza → motivo_inhabilitacion
 * y actualiza los CHECK constraints de PostgreSQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InhabilitadoSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            eliminarConstraintSiExiste("habitaciones_estado_check");
            eliminarConstraintSiExiste("chk_habitacion_estado");

            migrarColumnaYMotivo();
            migrarEstadosPendienteLimpieza();

            jdbcTemplate.execute("""
                    ALTER TABLE habitaciones
                    ADD CONSTRAINT habitaciones_estado_check CHECK (
                        estado IN ('LIBRE', 'RESERVADO', 'OCUPADO', 'INHABILITADO')
                    )
                    """);

            eliminarConstraintSiExiste("habitaciones_motivo_pendiente_limpieza_check");
            eliminarConstraintSiExiste("chk_habitacion_motivo_limpieza");
            eliminarConstraintSiExiste("habitaciones_motivo_inhabilitacion_check");

            jdbcTemplate.execute("""
                    ALTER TABLE habitaciones
                    ADD CONSTRAINT habitaciones_motivo_inhabilitacion_check CHECK (
                        motivo_inhabilitacion IS NULL
                        OR motivo_inhabilitacion IN ('POST_CHECKOUT', 'ADECUACION_PROGRAMADA')
                    )
                    """);

            log.info("Migración INHABILITADO aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración INHABILITADO: {}", ex.getMessage(), ex);
        }
    }

    private void migrarEstadosPendienteLimpieza() {
        int estados = jdbcTemplate.update("""
                UPDATE habitaciones SET estado = 'INHABILITADO' WHERE estado = 'PENDIENTE_LIMPIEZA'
                """);
        if (estados > 0) {
            log.info("Migración inhabilitado: {} habitación(es) PENDIENTE_LIMPIEZA → INHABILITADO", estados);
        }
    }

    private void migrarColumnaYMotivo() {
        jdbcTemplate.execute(
                "ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS motivo_inhabilitacion VARCHAR(30)"
        );

        if (columnaExiste("motivo_pendiente_limpieza")) {
            jdbcTemplate.update("""
                    UPDATE habitaciones
                    SET motivo_inhabilitacion = CASE
                        WHEN motivo_pendiente_limpieza = 'ADECUACION_PROGRAMADA' THEN 'ADECUACION_PROGRAMADA'
                        ELSE 'POST_CHECKOUT'
                    END
                    WHERE motivo_inhabilitacion IS NULL
                      AND motivo_pendiente_limpieza IS NOT NULL
                    """);
        }

        jdbcTemplate.update("""
                UPDATE habitaciones
                SET motivo_inhabilitacion = 'POST_CHECKOUT'
                WHERE motivo_inhabilitacion IS NULL
                  AND estado IN ('PENDIENTE_LIMPIEZA', 'INHABILITADO')
                """);
    }

    private void eliminarConstraintSiExiste(String nombre) {
        jdbcTemplate.execute("ALTER TABLE habitaciones DROP CONSTRAINT IF EXISTS " + nombre);
    }

    private boolean columnaExiste(String nombreColumna) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'habitaciones'
                  AND column_name = ?
                """,
                Integer.class,
                nombreColumna
        );
        return count != null && count > 0;
    }
}
