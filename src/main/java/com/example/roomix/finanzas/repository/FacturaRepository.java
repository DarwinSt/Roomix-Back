package com.example.roomix.finanzas.repository;

import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long>, JpaSpecificationExecutor<Factura> {

    Optional<Factura> findFirstByReservaIdAndEstadoNot(Long reservaId, EstadoFactura estado);

    Optional<Factura> findFirstByEventoIdAndEstadoNot(Long eventoId, EstadoFactura estado);

    List<Factura> findByHuespedIdOrderByFechaEmisionDesc(Long huespedId);

    @Query("SELECT COALESCE(SUM(f.total - f.totalPagado), 0) FROM Factura f WHERE f.estado IN :estados")
    BigDecimal saldoPendiente(@Param("estados") List<EstadoFactura> estados);

    static Specification<Factura> conFiltros(
            EstadoFactura estado,
            LocalDateTime desde,
            LocalDateTime hasta,
            String texto
    ) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThan(root.get("fechaEmision"), hasta));
            }
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("numero")), patron),
                        cb.like(cb.lower(root.get("clienteNombre")), patron),
                        cb.like(cb.lower(cb.coalesce(root.get("clienteDocumento"), "")), patron)
                ));
            }
            return cb.and(predicados.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
