package com.example.roomix.evento.domain;

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

import java.math.BigDecimal;

/** Espacio del hotel disponible para eventos (salones, terrazas, zona de piscina). */
@Entity
@Table(name = "salones", uniqueConstraints = @UniqueConstraint(name = "uk_salon_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(length = 300)
    private String descripcion;

    /** Tarifa de referencia por hora. */
    @Column(name = "tarifa_hora", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaHora;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
