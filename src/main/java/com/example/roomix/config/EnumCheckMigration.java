package com.example.roomix.config;

import com.example.roomix.auth.domain.Permiso;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.OrigenItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hibernate crea en PostgreSQL un CHECK con los valores de cada enum cuando crea la tabla, y con ddl-auto=update no lo
 * vuelve a generar. Al agregar valores (venta de productos, permiso VENTAS) la base existente los rechazaría, así que
 * aquí se rehace cada CHECK con los valores actuales.
 */
@Component
@Order(16)
@RequiredArgsConstructor
@Slf4j
public class EnumCheckMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!esPostgres()) {
            return;
        }
        actualizar("movimientos_financieros", "categoria", CategoriaFinanciera.class);
        actualizar("factura_items", "origen", OrigenItem.class);
        actualizar("usuario_permisos", "permiso", Permiso.class);
    }

    private void actualizar(String tabla, String columna, Class<? extends Enum<?>> tipo) {
        String nombre = tabla + "_" + columna + "_check";
        String valores = Arrays.stream(tipo.getEnumConstants()).map(e -> "'" + e.name() + "'").collect(Collectors.joining(", "));
        try {
            jdbcTemplate.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT IF EXISTS " + nombre);
            jdbcTemplate.execute("ALTER TABLE " + tabla + " ADD CONSTRAINT " + nombre + " CHECK (" + columna + " IN (" + valores + "))");
        } catch (Exception ex) {
            log.warn("No se pudo actualizar {}: {}", nombre, ex.getMessage());
        }
    }

    private boolean esPostgres() {
        try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
            return c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres");
        } catch (Exception ex) {
            return false;
        }
    }
}
