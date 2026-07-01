package com.example.roomix.huesped.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HuespedSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS huespedes (
                        id BIGSERIAL PRIMARY KEY,
                        nombre VARCHAR(80) NOT NULL,
                        apellidos VARCHAR(120) NOT NULL,
                        tipo_documento VARCHAR(20) NOT NULL,
                        numero_documento VARCHAR(40) NOT NULL,
                        email VARCHAR(120) NOT NULL,
                        telefono VARCHAR(30) NOT NULL,
                        nacionalidad VARCHAR(80),
                        fecha_nacimiento DATE,
                        notas VARCHAR(500),
                        activo BOOLEAN NOT NULL DEFAULT TRUE,
                        fecha_hora_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
                        fecha_hora_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT NOW(),
                        CONSTRAINT uk_huesped_documento UNIQUE (numero_documento)
                    )
                    """);

            jdbcTemplate.execute("ALTER TABLE habitaciones ADD COLUMN IF NOT EXISTS huesped_id BIGINT");
            jdbcTemplate.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (
                            SELECT 1 FROM pg_constraint WHERE conname = 'fk_habitacion_huesped'
                        ) THEN
                            ALTER TABLE habitaciones
                                ADD CONSTRAINT fk_habitacion_huesped
                                FOREIGN KEY (huesped_id) REFERENCES huespedes(id)
                                ON DELETE SET NULL;
                        END IF;
                    END $$
                    """);

            log.info("Migración de huéspedes aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración de huéspedes: {}", ex.getMessage(), ex);
        }
    }
}
