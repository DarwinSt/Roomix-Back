package com.example.roomix.caja.repository;

import com.example.roomix.caja.domain.EstadoTurno;
import com.example.roomix.caja.domain.TurnoCaja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    Optional<TurnoCaja> findFirstByEstado(EstadoTurno estado);

    Optional<TurnoCaja> findFirstByEstadoOrderByFechaHoraCierreDesc(EstadoTurno estado);

    @Query("""
            SELECT t FROM TurnoCaja t
            WHERE (:cajero IS NULL OR LOWER(t.cajero) = LOWER(:cajero))
              AND t.fechaHoraApertura >= :desde AND t.fechaHoraApertura < :hasta
            ORDER BY t.fechaHoraApertura DESC
            """)
    Page<TurnoCaja> buscar(@Param("cajero") String cajero, @Param("desde") LocalDateTime desde,
                           @Param("hasta") LocalDateTime hasta, Pageable pageable);
}
