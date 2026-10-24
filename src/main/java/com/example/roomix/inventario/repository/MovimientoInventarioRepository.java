package com.example.roomix.inventario.repository;

import com.example.roomix.inventario.domain.MovimientoInventario;
import com.example.roomix.inventario.domain.TipoMovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("""
            SELECT m FROM MovimientoInventario m JOIN FETCH m.articulo
            WHERE m.articulo.id = :articuloId
            ORDER BY m.fechaHora DESC, m.id DESC
            """)
    List<MovimientoInventario> deArticulo(@Param("articuloId") Long articuloId);

    @Query(value = """
            SELECT m FROM MovimientoInventario m JOIN FETCH m.articulo a
            WHERE m.fechaHora >= :desde AND m.fechaHora < :hasta
              AND (:tipo IS NULL OR m.tipo = :tipo)
            ORDER BY m.fechaHora DESC, m.id DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MovimientoInventario m
            WHERE m.fechaHora >= :desde AND m.fechaHora < :hasta
              AND (:tipo IS NULL OR m.tipo = :tipo)
            """)
    Page<MovimientoInventario> enRango(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipo") TipoMovimientoStock tipo,
            Pageable pageable
    );

    /** Artículos con más salidas en el periodo. Filas: [articuloId, nombre, unidad, cantidad, precioUnitario]. */
    @Query("""
            SELECT a.id, a.nombre, a.unidadMedida, SUM(m.cantidad), a.precioUnitario
            FROM MovimientoInventario m JOIN m.articulo a
            WHERE m.tipo = com.example.roomix.inventario.domain.TipoMovimientoStock.SALIDA
              AND m.fechaHora >= :desde AND m.fechaHora < :hasta
            GROUP BY a.id, a.nombre, a.unidadMedida, a.precioUnitario
            ORDER BY SUM(m.cantidad) DESC
            """)
    List<Object[]> masUsados(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
