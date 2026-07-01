package com.example.roomix.habitacion.repository;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    Optional<Habitacion> findByNumero(String numero);

    boolean existsByNumeroAndIdNot(String numero, Long id);

    List<Habitacion> findByEstado(EstadoHabitacion estado);
}
