package com.example.roomix.reserva.repository;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.domain.TarifaTipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaTipoHabitacionRepository extends JpaRepository<TarifaTipoHabitacion, TipoHabitacion> {
}
