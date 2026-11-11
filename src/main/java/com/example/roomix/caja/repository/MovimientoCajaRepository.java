package com.example.roomix.caja.repository;

import com.example.roomix.caja.domain.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByTurnoIdOrderByFechaHoraAsc(Long turnoId);
}
