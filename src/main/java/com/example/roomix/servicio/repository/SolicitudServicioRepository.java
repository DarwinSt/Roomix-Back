package com.example.roomix.servicio.repository;

import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.domain.SolicitudServicio;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SolicitudServicioRepository
        extends JpaRepository<SolicitudServicio, Long>, JpaSpecificationExecutor<SolicitudServicio> {

    List<SolicitudServicio> findByReservaIdAndEstadoNotOrderByFechaHoraSolicitudAsc(Long reservaId, EstadoSolicitud estado);

    long countByEstadoIn(Collection<EstadoSolicitud> estados);

    /** Servicios más solicitados en el periodo. Filas: [nombre, cantidad, total]. */
    @Query("""
            SELECT s.servicio.nombre, SUM(s.cantidad), SUM(s.total) FROM SolicitudServicio s
            WHERE s.estado <> com.example.roomix.servicio.domain.EstadoSolicitud.CANCELADA
              AND s.fechaHoraSolicitud >= :desde AND s.fechaHoraSolicitud < :hasta
            GROUP BY s.servicio.nombre
            ORDER BY SUM(s.total) DESC
            """)
    List<Object[]> ranking(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    static Specification<SolicitudServicio> conFiltros(
            EstadoSolicitud estado,
            Long reservaId,
            Long huespedId,
            LocalDateTime desde,
            LocalDateTime hasta,
            Boolean activas
    ) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (reservaId != null) {
                predicados.add(cb.equal(root.get("reserva").get("id"), reservaId));
            }
            if (huespedId != null) {
                predicados.add(cb.equal(root.get("huesped").get("id"), huespedId));
            }
            if (Boolean.TRUE.equals(activas)) {
                predicados.add(root.get("estado").in(EstadoSolicitud.SOLICITADA, EstadoSolicitud.EN_PROCESO));
            }
            if (desde != null || hasta != null) {
                var fecha = cb.coalesce(root.<LocalDateTime>get("fechaHoraProgramada"), root.<LocalDateTime>get("fechaHoraSolicitud"));
                if (desde != null) {
                    predicados.add(cb.greaterThanOrEqualTo(fecha, desde));
                }
                if (hasta != null) {
                    predicados.add(cb.lessThan(fecha, hasta));
                }
            }
            return cb.and(predicados.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
