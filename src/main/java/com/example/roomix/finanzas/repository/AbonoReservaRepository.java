package com.example.roomix.finanzas.repository;

import com.example.roomix.finanzas.domain.AbonoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonoReservaRepository extends JpaRepository<AbonoReserva, Long> {

    List<AbonoReserva> findByReservaIdOrderByFechaHoraAsc(Long reservaId);
}
