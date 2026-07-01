package com.example.roomix.inventario.repository;

import com.example.roomix.inventario.domain.ArticuloInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticuloInventarioRepository extends JpaRepository<ArticuloInventario, Long> {

    @Query("SELECT a FROM ArticuloInventario a JOIN FETCH a.categoria")
    List<ArticuloInventario> findAllWithCategoria();

    @Query("SELECT a FROM ArticuloInventario a JOIN FETCH a.categoria WHERE a.id = :id")
    Optional<ArticuloInventario> findByIdWithCategoria(@Param("id") Long id);

    Optional<ArticuloInventario> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("SELECT a FROM ArticuloInventario a JOIN FETCH a.categoria WHERE a.categoria.id = :categoriaId")
    List<ArticuloInventario> findByCategoriaId(@Param("categoriaId") Long categoriaId);

    @Query("SELECT a FROM ArticuloInventario a JOIN FETCH a.categoria WHERE a.activo = :activo")
    List<ArticuloInventario> findByActivo(@Param("activo") boolean activo);

    boolean existsByCategoriaIdAndActivoTrue(Long categoriaId);

    long countByCategoriaId(Long categoriaId);

    @Query("""
            SELECT a FROM ArticuloInventario a JOIN FETCH a.categoria
            WHERE a.activo = true
              AND a.cantidadMinima IS NOT NULL
              AND a.cantidad <= a.cantidadMinima
            """)
    List<ArticuloInventario> findConStockBajo();
}
