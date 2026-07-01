package com.example.roomix.reserva.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ReservaSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tarifas_tipo_habitacion (
                        tipo_habitacion VARCHAR(30) PRIMARY KEY,
                        precio_noche DECIMAL(10,2) NOT NULL,
                        fecha_hora_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT NOW()
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS reservas (
                        id BIGSERIAL PRIMARY KEY,
                        habitacion_id BIGINT NOT NULL,
                        huesped_id BIGINT NOT NULL,
                        fecha_entrada DATE NOT NULL,
                        fecha_salida DATE NOT NULL,
                        cantidad_noches INT NOT NULL,
                        estado_reserva VARCHAR(20) NOT NULL,
                        tarifa_noche DECIMAL(10,2) NOT NULL,
                        total_estimado DECIMAL(10,2) NOT NULL,
                        hora_real_check_in TIMESTAMP,
                        hora_real_check_out TIMESTAMP,
                        fecha_hora_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
                        fecha_hora_ultima_actualizacion TIMESTAMP NOT NULL DEFAULT NOW(),
                        CONSTRAINT fk_reserva_habitacion
                            FOREIGN KEY (habitacion_id) REFERENCES habitaciones(id),
                        CONSTRAINT fk_reserva_huesped
                            FOREIGN KEY (huesped_id) REFERENCES huespedes(id)
                    )
                    """);

            seedTarifas();
            migrarReservasActivas();

            log.info("Migración de reservas y tarifas aplicada correctamente");
        } catch (Exception ex) {
            log.error("Error al aplicar migración de reservas: {}", ex.getMessage(), ex);
        }
    }

    private void seedTarifas() {
        insertarTarifaSiFalta("INDIVIDUAL", 80);
        insertarTarifaSiFalta("DOBLE", 120);
        insertarTarifaSiFalta("TRIPLE", 150);
        insertarTarifaSiFalta("SUITE", 250);
        insertarTarifaSiFalta("FAMILIAR", 180);
        insertarTarifaSiFalta("EJECUTIVA", 200);
    }

    private void insertarTarifaSiFalta(String tipo, double precio) {
        jdbcTemplate.update("""
                INSERT INTO tarifas_tipo_habitacion (tipo_habitacion, precio_noche)
                VALUES (?, ?)
                ON CONFLICT (tipo_habitacion) DO NOTHING
                """, tipo, precio);
    }

    private void migrarReservasActivas() {
        jdbcTemplate.update("""
                INSERT INTO reservas (
                    habitacion_id, huesped_id, fecha_entrada, fecha_salida, cantidad_noches,
                    estado_reserva, tarifa_noche, total_estimado, hora_real_check_in, hora_real_check_out
                )
                SELECT
                    h.id,
                    h.huesped_id,
                    h.fecha_entrada,
                    h.fecha_salida,
                    h.cantidad_noches,
                    h.estado_reserva,
                    COALESCE(t.precio_noche, 100),
                    COALESCE(t.precio_noche, 100) * h.cantidad_noches,
                    h.hora_real_check_in,
                    h.hora_real_check_out
                FROM habitaciones h
                LEFT JOIN tarifas_tipo_habitacion t ON t.tipo_habitacion = h.tipo_habitacion
                WHERE h.estado_reserva IN ('CONFIRMADA', 'EN_CURSO')
                  AND h.huesped_id IS NOT NULL
                  AND h.fecha_entrada IS NOT NULL
                  AND h.fecha_salida IS NOT NULL
                  AND h.cantidad_noches IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM reservas r
                      WHERE r.habitacion_id = h.id
                        AND r.estado_reserva IN ('CONFIRMADA', 'EN_CURSO')
                  )
                """);
    }
}
