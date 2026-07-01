package com.example.roomix.incidencia.repository;

import com.example.roomix.incidencia.domain.ContextoLimpieza;
import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.domain.Incidencia;
import com.example.roomix.incidencia.domain.TipoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Query("""
            SELECT DISTINCT i FROM Incidencia i
            LEFT JOIN FETCH i.habitacion h
            LEFT JOIN FETCH i.personalAsignado p
            LEFT JOIN FETCH i.tareas t
            WHERE (:estado IS NULL OR i.estado = :estado)
              AND (:habitacionId IS NULL OR h.id = :habitacionId)
              AND (:personalId IS NULL OR p.id = :personalId)
              AND (:activas IS NULL OR :activas = false OR i.estado NOT IN :cerradas)
            ORDER BY i.fechaHoraCreacion DESC
            """)
    List<Incidencia> buscarConFiltros(
            @Param("estado") EstadoIncidencia estado,
            @Param("habitacionId") Long habitacionId,
            @Param("personalId") Long personalId,
            @Param("activas") Boolean activas,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT i FROM Incidencia i
            LEFT JOIN FETCH i.habitacion h
            LEFT JOIN FETCH i.personalAsignado p
            LEFT JOIN FETCH i.tareas t
            WHERE i.id = :id
            """)
    Optional<Incidencia> findDetalleById(@Param("id") Long id);

    boolean existsByHabitacionIdAndTipoAndEstadoNotIn(
            Long habitacionId,
            TipoIncidencia tipo,
            Collection<EstadoIncidencia> estados
    );

    @Query("""
            SELECT i FROM Incidencia i
            LEFT JOIN FETCH i.habitacion h
            LEFT JOIN FETCH i.personalAsignado p
            LEFT JOIN FETCH i.tareas t
            WHERE h.id = :habitacionId
              AND i.estado NOT IN :cerradas
            ORDER BY i.fechaHoraCreacion DESC
            """)
    List<Incidencia> findActivasPorHabitacion(
            @Param("habitacionId") Long habitacionId,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    long countByHabitacionIdAndEstadoNotIn(Long habitacionId, Collection<EstadoIncidencia> estados);

    List<Incidencia> findByHabitacionId(Long habitacionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    DELETE FROM incidencia_tareas
                    WHERE incidencia_id IN (
                        SELECT id FROM incidencias WHERE habitacion_id = :habitacionId
                    )
                    """,
            nativeQuery = true
    )
    void deleteTareasByHabitacionId(@Param("habitacionId") Long habitacionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM incidencias WHERE habitacion_id = :habitacionId", nativeQuery = true)
    void deleteByHabitacionId(@Param("habitacionId") Long habitacionId);

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.habitacion.id = :habitacionId
              AND i.tipo = :tipoMantenimiento
              AND i.estado NOT IN :cerradas
              AND i.fechaHoraProgramada IS NOT NULL
              AND i.fechaHoraProgramada >= :inicioDia
              AND i.fechaHoraProgramada < :finDia
            """)
    boolean tieneMantenimientoActivoProgramadoPara(
            @Param("habitacionId") Long habitacionId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("tipoMantenimiento") TipoIncidencia tipoMantenimiento,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.habitacion.id = :habitacionId
              AND i.tipo = :tipoLimpieza
              AND i.contextoLimpieza = :contexto
              AND i.estado NOT IN :cerradas
            """)
    boolean existsLimpiezaActivaConContexto(
            @Param("habitacionId") Long habitacionId,
            @Param("tipoLimpieza") TipoIncidencia tipoLimpieza,
            @Param("contexto") ContextoLimpieza contexto,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.habitacion.id = :habitacionId
              AND i.tipo = :tipoLimpieza
              AND i.contextoLimpieza = :contextoPostCheckout
              AND i.estado NOT IN :cerradas
            """)
    boolean existsLimpiezaPostCheckoutActiva(
            @Param("habitacionId") Long habitacionId,
            @Param("tipoLimpieza") TipoIncidencia tipoLimpieza,
            @Param("contextoPostCheckout") ContextoLimpieza contextoPostCheckout,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.habitacion.id = :habitacionId
              AND i.tipo = :tipoLimpieza
              AND i.estado NOT IN :cerradas
            """)
    boolean existsLimpiezaActiva(
            @Param("habitacionId") Long habitacionId,
            @Param("tipoLimpieza") TipoIncidencia tipoLimpieza,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.habitacion.id = :habitacionId
              AND i.tipo = :tipoMantenimiento
              AND i.estado NOT IN :cerradas
              AND i.fechaHoraProgramada IS NOT NULL
              AND i.fechaHoraProgramada >= :inicioRango
              AND i.fechaHoraProgramada < :finRango
            """)
    boolean tieneMantenimientoActivoEnRango(
            @Param("habitacionId") Long habitacionId,
            @Param("inicioRango") LocalDateTime inicioRango,
            @Param("finRango") LocalDateTime finRango,
            @Param("tipoMantenimiento") TipoIncidencia tipoMantenimiento,
            @Param("cerradas") Collection<EstadoIncidencia> cerradas
    );

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Incidencia i
            WHERE i.personalAsignado.id = :personalId
              AND i.estado IN :estadosOcupado
              AND i.id <> :incidenciaIdExcluida
            """)
    boolean personalTieneIncidenciaActiva(
            @Param("personalId") Long personalId,
            @Param("estadosOcupado") Collection<EstadoIncidencia> estadosOcupado,
            @Param("incidenciaIdExcluida") Long incidenciaIdExcluida
    );

    @Query("""
            SELECT DISTINCT i.personalAsignado.id
            FROM Incidencia i
            WHERE i.personalAsignado IS NOT NULL
              AND i.estado IN :estadosOcupado
            """)
    Set<Long> findPersonalIdsOcupados(@Param("estadosOcupado") Collection<EstadoIncidencia> estadosOcupado);
}
