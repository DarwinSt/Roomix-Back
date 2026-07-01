package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HabitacionPrecioMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    ALTER TABLE habitaciones
                    ADD COLUMN IF NOT EXISTS precio_noche DECIMAL(10,2)
                    """);

            jdbcTemplate.update("""
                    UPDATE habitaciones h
                    SET precio_noche = t.precio_noche
                    FROM tarifas_tipo_habitacion t
                    WHERE h.tipo_habitacion = t.tipo_habitacion
                      AND h.precio_noche IS NULL
                    """);

            log.info("Migración de precio por habitación aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración de precio por habitación: {}", ex.getMessage(), ex);
        }
    }
}
