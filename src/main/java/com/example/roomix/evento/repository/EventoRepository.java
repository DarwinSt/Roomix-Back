package com.example.roomix.evento.repository;

import com.example.roomix.evento.domain.Evento;
import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Eventos que se cruzan con [inicio, fin) en el mismo salón (excluye el propio evento al editar). */
    @Query("""
            SELECT COUNT(e) > 0 FROM Evento e
            WHERE e.salon.id = :salonId
              AND e.estado IN :estados
              AND e.id <> :excluirId
              AND e.fechaHoraInicio < :fin
              AND e.fechaHoraFin > :inicio
            """)
    boolean existeCruce(
            @Param("salonId") Long salonId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") Long excluirId,
            @Param("estados") Collection<EstadoEvento> estados
    );

    @Query("""
            SELECT e FROM Evento e
            WHERE e.fechaHoraInicio < :hasta AND e.fechaHoraFin >= :desde
            ORDER BY e.fechaHoraInicio ASC
            """)
    List<Evento> enRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    List<Evento> findByEstadoInAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(
            Collection<EstadoEvento> estados,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
