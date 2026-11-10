package com.example.roomix.reserva.repository;

import com.example.roomix.reserva.domain.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    List<Promocion> findAllByOrderByActivaDescNombreAsc();

    List<Promocion> findByActivaTrueOrderByNombreAsc();
}
