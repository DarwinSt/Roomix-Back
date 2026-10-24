package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Completa piso y capacidad en habitaciones creadas antes de existir esos campos:
 * piso = primer dígito del número (205 → 2) y capacidad según el tipo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HabitacionPlanoMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int pisos = jdbcTemplate.update("""
                    UPDATE habitaciones
                    SET piso = CAST(SUBSTRING(numero, 1, 1) AS INTEGER)
                    WHERE piso IS NULL AND LENGTH(numero) >= 3 AND SUBSTRING(numero, 1, 1) BETWEEN '0' AND '9'
                    """);
            jdbcTemplate.update("UPDATE habitaciones SET piso = 1 WHERE piso IS NULL");
            int capacidades = jdbcTemplate.update("""
                    UPDATE habitaciones
                    SET capacidad = CASE tipo_habitacion
                        WHEN 'INDIVIDUAL' THEN 1
                        WHEN 'DOBLE' THEN 2
                        WHEN 'EJECUTIVA' THEN 2
                        WHEN 'TRIPLE' THEN 3
                        WHEN 'SUITE' THEN 3
                        WHEN 'FAMILIAR' THEN 5
                        ELSE 2 END
                    WHERE capacidad IS NULL
                    """);
            if (pisos > 0 || capacidades > 0) {
                log.info("Plano de habitaciones: {} piso(s) y {} capacidad(es) completados", pisos, capacidades);
            }
        } catch (Exception ex) {
            log.warn("Migración de plano de habitaciones: {}", ex.getMessage());
        }
    }
}
