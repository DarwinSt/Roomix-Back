package com.example.roomix.caja.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Turno de caja de la recepción: se abre con la base (efectivo) que se recibe, acumula los pagos, ingresos y egresos
 * que registra el cajero y se cierra con el arqueo (conteo del efectivo), la base que queda para el siguiente turno
 * y el valor que se entrega. El hotel tiene una sola caja: solo puede haber un turno abierto a la vez.
 */
@Entity
@Table(name = "turnos_caja", indexes = {
        @Index(name = "idx_turno_caja_estado", columnList = "estado"),
        @Index(name = "idx_turno_caja_cajero", columnList = "cajero")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que abrió el turno y responde por la caja. */
    @Column(nullable = false, length = 50)
    private String cajero;

    @Column(name = "cajero_nombre", nullable = false, length = 120)
    private String cajeroNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoTurno estado;

    @Column(name = "fecha_hora_apertura", nullable = false)
    private LocalDateTime fechaHoraApertura;

    /** Efectivo recibido al abrir. */
    @Column(name = "base_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseInicial;

    /** Base que dejó el turno anterior (null si es el primero); si difiere de la recibida se exige observación. */
    @Column(name = "base_esperada", precision = 14, scale = 2)
    private BigDecimal baseEsperada;

    @Column(name = "observacion_apertura", length = 300)
    private String observacionApertura;

    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    @Column(name = "cerrado_por", length = 50)
    private String cerradoPor;

    /** Efectivo que debía haber al cerrar: base + ingresos − egresos en efectivo + aportes − retiros. */
    @Column(name = "efectivo_esperado", precision = 14, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 14, scale = 2)
    private BigDecimal efectivoContado;

    /** Contado − esperado: positivo es sobrante y negativo, faltante. */
    @Column(precision = 14, scale = 2)
    private BigDecimal diferencia;

    /** Efectivo que queda en la caja como base del siguiente turno. */
    @Column(name = "base_siguiente", precision = 14, scale = 2)
    private BigDecimal baseSiguiente;

    /** Efectivo que se entrega a la administración (contado − base siguiente). */
    @Column(name = "valor_entregado", precision = 14, scale = 2)
    private BigDecimal valorEntregado;

    /** Persona que recibe la caja o el dinero entregado. */
    @Column(name = "recibido_por", length = 120)
    private String recibidoPor;

    @Column(name = "observacion_cierre", length = 500)
    private String observacionCierre;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "turnos_caja_conteo", joinColumns = @JoinColumn(name = "turno_id"))
    @OrderColumn(name = "orden")
    @Builder.Default
    private List<ConteoDenominacion> conteo = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "turnos_caja_totales", joinColumns = @JoinColumn(name = "turno_id"))
    @OrderColumn(name = "orden")
    @Builder.Default
    private List<TotalMedioPago> totalesCierre = new ArrayList<>();

    @Column(name = "retiros_cierre", precision = 14, scale = 2)
    private BigDecimal retirosCierre;

    @Column(name = "aportes_cierre", precision = 14, scale = 2)
    private BigDecimal aportesCierre;

    @Column(name = "movimientos_cierre")
    private Integer movimientosCierre;

    public boolean abierto() {
        return estado == EstadoTurno.ABIERTO;
    }
}
