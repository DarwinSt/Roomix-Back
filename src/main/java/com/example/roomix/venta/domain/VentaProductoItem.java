package com.example.roomix.venta.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Producto vendido: guarda el nombre y el precio del momento de la venta. */
@Entity
@Table(name = "ventas_productos_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaProductoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private VentaProducto venta;

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    /** Costo unitario del artículo al momento de la venta (para el margen); null si no se conocía. */
    @Column(name = "costo_unitario", precision = 12, scale = 2)
    private BigDecimal costoUnitario;
}
