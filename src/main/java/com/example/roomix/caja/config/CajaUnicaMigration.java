package com.example.roomix.caja.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * El hotel tiene una sola caja: la base de datos no admite dos turnos abiertos aunque dos personas pulsen «Abrir turno»
 * al mismo tiempo (índice único parcial de PostgreSQL sobre los turnos en estado ABIERTO).
 */
@Component
@Order(17)
@RequiredArgsConstructor
@Slf4j
public class CajaUnicaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres")) {
                return;
            }
        } catch (Exception ex) {
            return;
        }
        try {
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_turno_caja_abierto ON turnos_caja (estado) WHERE estado = 'ABIERTO'");
        } catch (Exception ex) {
            log.warn("No se pudo crear el índice de caja única (¿hay dos turnos abiertos?): {}", ex.getMessage());
        }
    }
}
