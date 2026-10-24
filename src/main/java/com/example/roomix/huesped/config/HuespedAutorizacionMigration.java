package com.example.roomix.huesped.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Al incorporar la autorización de datos (Ley 1581), los huéspedes registrados antes de la
 * funcionalidad quedan sin marca. Si ningún huésped tiene autorización registrada (primer despliegue
 * de la versión), se asume que fueron registrados con autorización en papel y se toma como fecha
 * la de su registro. A partir de ahí, la autorización es obligatoria en el formulario.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HuespedAutorizacionMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer conAutorizacion = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM huespedes WHERE autoriza_tratamiento_datos = TRUE", Integer.class);
            if (conAutorizacion != null && conAutorizacion == 0) {
                int actualizados = jdbcTemplate.update("""
                        UPDATE huespedes
                        SET autoriza_tratamiento_datos = TRUE,
                            fecha_hora_autorizacion = fecha_hora_creacion
                        WHERE fecha_hora_autorizacion IS NULL
                        """);
                if (actualizados > 0) {
                    log.info("Autorización de datos: {} huésped(es) previo(s) marcados con su fecha de registro", actualizados);
                }
            }
        } catch (Exception ex) {
            log.warn("Migración de autorización de datos: {}", ex.getMessage());
        }
    }
}
