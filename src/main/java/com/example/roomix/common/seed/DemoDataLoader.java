package com.example.roomix.common.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Carga {@code db/seed-data.sql} cuando {@code app.seed.enabled=true} (APP_SEED_ENABLED).
 * <p>
 * Se ejecuta en {@link ApplicationReadyEvent}, es decir, después de que Hibernate creó las tablas
 * y de que corrieron todas las migraciones ({@code ApplicationRunner}). Solo actúa si la tabla
 * de habitaciones está vacía, para no sobrescribir datos de un hotel en operación.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataLoader {

    private static final String SCRIPT = "db/seed-data.sql";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        Integer habitaciones = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM habitaciones", Integer.class);
        if (habitaciones != null && habitaciones > 0) {
            log.info("Datos iniciales omitidos: la base ya tiene {} habitación(es)", habitaciones);
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(SCRIPT));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
        log.info("Datos iniciales cargados desde {}", SCRIPT);
    }
}
