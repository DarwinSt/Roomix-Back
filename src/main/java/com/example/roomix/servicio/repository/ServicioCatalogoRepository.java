package com.example.roomix.servicio.repository;

import com.example.roomix.servicio.domain.ServicioCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicioCatalogoRepository extends JpaRepository<ServicioCatalogo, Long> {

    List<ServicioCatalogo> findAllByOrderByCategoriaAscNombreAsc();

    List<ServicioCatalogo> findByActivoTrueOrderByCategoriaAscNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
