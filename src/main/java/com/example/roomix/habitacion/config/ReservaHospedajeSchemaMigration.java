package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migra fecha_hora_reservacion → campos de reserva/hospedaje.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservaHospedajeSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS fecha_entrada DATE");
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS fecha_salida DATE");
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS cantidad_noches INTEGER");
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS estado_reserva VARCHAR(20)");
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS hora_real_check_in TIMESTAMP");
            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS hora_real_check_out TIMESTAMP");

            if (columnaExiste("fecha_hora_reservacion")) {
                jdbcTemplate.update("""
                        UPDATE habitaciones
                        SET fecha_entrada = CAST(fecha_hora_reservacion AS DATE),
                            fecha_salida = CAST(fecha_hora_reservacion AS DATE) + INTERVAL '1 day',
                            cantidad_noches = 1,
                            estado_reserva = CASE
                                WHEN estado = 'OCUPADO' THEN 'EN_CURSO'
                                WHEN estado = 'INHABILITADO' THEN 'FINALIZADA'
                                WHEN estado IN ('RESERVADO', 'LIBRE') THEN 'CONFIRMADA'
                                ELSE NULL
                            END
                        WHERE fecha_entrada IS NULL
                          AND fecha_hora_reservacion IS NOT NULL
                        """);
            }

            log.info("Migración reserva/hospedaje aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración reserva/hospedaje: {}", ex.getMessage(), ex);
        }
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
