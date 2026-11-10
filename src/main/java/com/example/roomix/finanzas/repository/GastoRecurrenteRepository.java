package com.example.roomix.finanzas.repository;

import com.example.roomix.finanzas.domain.GastoRecurrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoRecurrenteRepository extends JpaRepository<GastoRecurrente, Long> {

    List<GastoRecurrente> findAllByOrderByActivoDescDiaVencimientoAsc();

    List<GastoRecurrente> findByActivoTrue();
}
