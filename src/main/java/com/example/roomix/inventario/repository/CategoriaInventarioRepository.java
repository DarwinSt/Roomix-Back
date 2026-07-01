package com.example.roomix.inventario.repository;

import com.example.roomix.inventario.domain.CategoriaInventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaInventarioRepository extends JpaRepository<CategoriaInventario, Long> {

    Optional<CategoriaInventario> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<CategoriaInventario> findByActivoTrueOrderByNombreAsc();

    List<CategoriaInventario> findAllByOrderByNombreAsc();
}
