package com.example.roomix.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventario_categorias",
        uniqueConstraints = @UniqueConstraint(name = "uk_categoria_nombre", columnNames = "nombre")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 300)
    private String descripcion;

    /**
     * Ejemplos orientativos de qué artículos pertenecen a esta categoría.
     */
    @Column(name = "ejemplos_articulos", nullable = false, length = 500)
    private String ejemplosArticulos;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Categorías precargadas del sistema; no se eliminan, solo desactivan. */
    @Column(nullable = false)
    @Builder.Default
    private boolean predefinida = false;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;
}
