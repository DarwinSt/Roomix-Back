package com.example.roomix.personal.repository;

import com.example.roomix.personal.domain.Personal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    List<Personal> findByActivoTrueOrderByNombreAsc();

    List<Personal> findAllByOrderByNombreAsc();
}
