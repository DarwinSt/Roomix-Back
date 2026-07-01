package com.example.roomix.huesped.repository;

import com.example.roomix.huesped.domain.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    Optional<Huesped> findByNumeroDocumentoIgnoreCase(String numeroDocumento);

    boolean existsByNumeroDocumentoIgnoreCaseAndIdNot(String numeroDocumento, Long id);

    @Query("""
            SELECT h FROM Huesped h
            WHERE (:activo IS NULL OR h.activo = :activo)
              AND (
                    :busqueda IS NULL OR :busqueda = '' OR
                    LOWER(h.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                    LOWER(h.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                    LOWER(h.numeroDocumento) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                    LOWER(h.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                    LOWER(h.telefono) LIKE LOWER(CONCAT('%', :busqueda, '%'))
              )
            ORDER BY h.apellidos ASC, h.nombre ASC
            """)
    List<Huesped> buscar(@Param("activo") Boolean activo, @Param("busqueda") String busqueda);
}
