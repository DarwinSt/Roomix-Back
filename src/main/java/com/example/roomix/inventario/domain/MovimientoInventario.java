package com.example.roomix.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historial de entradas, salidas y ajustes de stock: quién, cuándo, cuánto y por qué.
 */
@Entity
@Table(
        name = "inventario_movimientos",
        indexes = {
                @Index(name = "idx_movinv_articulo", columnList = "articulo_id"),
                @Index(name = "idx_movinv_fecha", columnList = "fecha_hora")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloInventario articulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimientoStock tipo;

    /** Unidades movidas (en un ajuste: diferencia entre el conteo y el stock anterior, puede ser negativa). */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "stock_resultante", nullable = false)
    private Integer stockResultante;

    @Column(name = "costo_unitario", precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(length = 200)
    private String motivo;

    @Column(length = 120)
    private String proveedor;

    @Column(name = "registrado_por", nullable = false, length = 50)
    private String registradoPor;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
