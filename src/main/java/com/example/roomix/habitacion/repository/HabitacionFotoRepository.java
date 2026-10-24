package com.example.roomix.habitacion.repository;

import com.example.roomix.habitacion.domain.HabitacionFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HabitacionFotoRepository extends JpaRepository<HabitacionFoto, Long> {

    /** Solo ids (sin cargar las imágenes). Filas: [habitacionId, fotoId]. */
    @Query("""
            SELECT f.habitacion.id, f.id FROM HabitacionFoto f
            WHERE f.habitacion.id IN :habitacionIds
            ORDER BY f.orden ASC, f.id ASC
            """)
    List<Object[]> idsPorHabitacion(@Param("habitacionIds") Collection<Long> habitacionIds);

    long countByHabitacionId(Long habitacionId);

    Optional<HabitacionFoto> findByIdAndHabitacionId(Long id, Long habitacionId);

    @Modifying
    @Query("DELETE FROM HabitacionFoto f WHERE f.habitacion.id = :habitacionId")
    void deleteByHabitacionId(@Param("habitacionId") Long habitacionId);
}
