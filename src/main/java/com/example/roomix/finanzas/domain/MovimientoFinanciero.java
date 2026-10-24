package com.example.roomix.finanzas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movimiento de caja (ingreso o egreso). Los ingresos por pagos de facturas y los egresos por
 * compras de inventario se registran automáticamente; los demás egresos, manualmente.
 * No se eliminan: se anulan con motivo para conservar la trazabilidad.
 */
@Entity
@Table(
        name = "movimientos_financieros",
        indexes = {
                @Index(name = "idx_movfin_fecha", columnList = "fecha"),
                @Index(name = "idx_movfin_tipo", columnList = "tipo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaFinanciera categoria;

    @Column(nullable = false, length = 200)
    private String concepto;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    /** Proveedor, cliente o tercero relacionado. */
    @Column(length = 150)
    private String tercero;

    /** Documento origen legible: FV-000012, COMPRA INV-5, EVT-3... */
    @Column(length = 60)
    private String referencia;

    @Column(name = "factura_id")
    private Long facturaId;

    @Column(name = "pago_id")
    private Long pagoId;

    @Column(name = "registrado_por", nullable = false, length = 50)
    private String registradoPor;

    @Column(nullable = false)
    @Builder.Default
    private boolean anulado = false;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;
}
