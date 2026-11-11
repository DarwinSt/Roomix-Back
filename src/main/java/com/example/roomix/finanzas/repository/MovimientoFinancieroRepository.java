package com.example.roomix.finanzas.repository;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MovimientoFinancieroRepository
        extends JpaRepository<MovimientoFinanciero, Long>, JpaSpecificationExecutor<MovimientoFinanciero> {

    List<MovimientoFinanciero> findByPagoIdAndAnuladoFalse(Long pagoId);

    /** Movimientos de un turno de caja (incluye anulados para el detalle). */
    List<MovimientoFinanciero> findByTurnoCajaIdOrderByFechaHoraCreacionAsc(Long turnoCajaId);

    List<MovimientoFinanciero> findByFacturaIdAndAnuladoFalse(Long facturaId);

    /** Egresos aún por pagar generados por un gasto recurrente. */
    List<MovimientoFinanciero> findByGastoRecurrenteIdAndPendienteTrueAndAnuladoFalse(Long gastoRecurrenteId);

    /** Totales por categoría en el periodo (sin anulados). Filas: [categoria, suma]. */
    @Query("""
            SELECT m.categoria, SUM(m.valor) FROM MovimientoFinanciero m
            WHERE m.anulado = false AND (m.pendiente IS NULL OR m.pendiente = false) AND m.fecha >= :desde AND m.fecha <= :hasta
            GROUP BY m.categoria
            """)
    List<Object[]> totalesPorCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Totales diarios por tipo en el periodo. Filas: [fecha, tipo, suma]. */
    @Query("""
            SELECT m.fecha, m.tipo, SUM(m.valor) FROM MovimientoFinanciero m
            WHERE m.anulado = false AND (m.pendiente IS NULL OR m.pendiente = false) AND m.fecha >= :desde AND m.fecha <= :hasta
            GROUP BY m.fecha, m.tipo
            ORDER BY m.fecha
            """)
    List<Object[]> totalesDiarios(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Egresos por pagar (no anulados). Filas: [cantidad, suma, cantidad vencidos, suma vencidos]. */
    @Query("""
            SELECT COUNT(m), COALESCE(SUM(m.valor), 0),
                   SUM(CASE WHEN m.fechaVencimiento < :hoy THEN 1 ELSE 0 END),
                   COALESCE(SUM(CASE WHEN m.fechaVencimiento < :hoy THEN m.valor ELSE 0 END), 0)
            FROM MovimientoFinanciero m WHERE m.anulado = false AND m.pendiente = true
            """)
    List<Object[]> resumenPorPagar(@Param("hoy") LocalDate hoy);

    static Specification<MovimientoFinanciero> conFiltros(
            TipoMovimiento tipo,
            CategoriaFinanciera categoria,
            LocalDate desde,
            LocalDate hasta,
            boolean incluirAnulados,
            String texto,
            Boolean pendiente
    ) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (tipo != null) {
                predicados.add(cb.equal(root.get("tipo"), tipo));
            }
            if (categoria != null) {
                predicados.add(cb.equal(root.get("categoria"), categoria));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            if (!incluirAnulados) {
                predicados.add(cb.isFalse(root.get("anulado")));
            }
            if (Boolean.TRUE.equals(pendiente)) {
                predicados.add(cb.isTrue(root.get("pendiente")));
            } else if (Boolean.FALSE.equals(pendiente)) {
                predicados.add(cb.or(cb.isNull(root.get("pendiente")), cb.isFalse(root.get("pendiente"))));
            }
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("concepto")), patron),
                        cb.like(cb.lower(cb.coalesce(root.get("tercero"), "")), patron),
                        cb.like(cb.lower(cb.coalesce(root.get("referencia"), "")), patron)
                ));
            }
            return cb.and(predicados.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
