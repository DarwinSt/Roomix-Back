package com.example.roomix.habitacion.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura ON DELETE CASCADE entre habitaciones, incidencias e incidencia_tareas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HabitacionIncidenciasCascadeMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            recrearFkCascade(
                    "incidencia_tareas",
                    "incidencia_id",
                    "incidencias",
                    "id",
                    "fk_incidencia_tareas_incidencia"
            );
            recrearFkCascade(
                    "incidencias",
                    "habitacion_id",
                    "habitaciones",
                    "id",
                    "fk_incidencias_habitacion"
            );
            log.info("Migración CASCADE habitaciones/incidencias aplicada");
        } catch (Exception ex) {
            log.warn("No se pudo aplicar CASCADE habitaciones/incidencias: {}", ex.getMessage());
        }
    }

    private void recrearFkCascade(
            String tablaHija,
            String columnaHija,
            String tablaPadre,
            String columnaPadre,
            String nombreConstraint
    ) {
        jdbcTemplate.execute(
                "ALTER TABLE " + tablaHija + " DROP CONSTRAINT IF EXISTS " + nombreConstraint
        );

        var constraints = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_name = kcu.constraint_name
                 AND tc.table_schema = kcu.table_schema
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND tc.table_schema = 'public'
                  AND tc.table_name = ?
                  AND kcu.column_name = ?
                """, String.class, tablaHija, columnaHija);

        for (String nombre : constraints) {
            if (!nombre.equals(nombreConstraint)) {
                jdbcTemplate.execute("ALTER TABLE " + tablaHija + " DROP CONSTRAINT IF EXISTS " + nombre);
            }
        }

        jdbcTemplate.execute("""
                ALTER TABLE %s
                ADD CONSTRAINT %s
                FOREIGN KEY (%s) REFERENCES %s(%s) ON DELETE CASCADE
                """.formatted(tablaHija, nombreConstraint, columnaHija, tablaPadre, columnaPadre));
    }
}
