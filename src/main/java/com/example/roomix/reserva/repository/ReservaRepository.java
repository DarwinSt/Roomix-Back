package com.example.roomix.reserva.repository;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.reserva.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
            FROM Reserva r
            WHERE r.habitacion.id = :habitacionId
              AND r.estadoReserva IN :estadosActivos
              AND r.fechaEntrada < :fechaSalida
              AND r.fechaSalida > :fechaEntrada
            """)
    boolean existeSolapamiento(
            @Param("habitacionId") Long habitacionId,
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("estadosActivos") Collection<EstadoReserva> estadosActivos
    );

    Optional<Reserva> findFirstByHabitacionIdAndEstadoReservaIn(
            Long habitacionId,
            Collection<EstadoReserva> estados
    );

    List<Reserva> findByHuespedIdOrderByFechaEntradaDesc(Long huespedId);

    List<Reserva> findByHabitacionIdOrderByFechaEntradaDesc(Long habitacionId);

    /** Igual que existeSolapamiento pero ignorando la propia reserva (al editar fechas). */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END
            FROM Reserva r
            WHERE r.habitacion.id = :habitacionId
              AND r.id <> :reservaId
              AND r.estadoReserva IN :estadosActivos
              AND r.fechaEntrada < :fechaSalida
              AND r.fechaSalida > :fechaEntrada
            """)
    boolean existeSolapamientoExcluyendo(
            @Param("habitacionId") Long habitacionId,
            @Param("reservaId") Long reservaId,
            @Param("fechaEntrada") LocalDate fechaEntrada,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("estadosActivos") Collection<EstadoReserva> estadosActivos
    );

    /** Llegadas o salidas previstas para un día. */
    List<Reserva> findByFechaEntradaAndEstadoReservaInOrderByHabitacionNumeroAsc(LocalDate fecha, Collection<EstadoReserva> estados);

    List<Reserva> findByFechaSalidaAndEstadoReservaInOrderByHabitacionNumeroAsc(LocalDate fecha, Collection<EstadoReserva> estados);

    /** Reservas que ocupan noches dentro de [desde, hasta) (para ocupación, ADR y RevPAR). */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.estadoReserva IN :estados
              AND r.fechaEntrada < :hasta
              AND r.fechaSalida > :desde
            """)
    List<Reserva> queOcupanRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estados") Collection<EstadoReserva> estados
    );
}
