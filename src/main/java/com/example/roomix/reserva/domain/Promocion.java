package com.example.roomix.reserva.domain;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Promoción configurable por el administrador. Aplica a una reserva cuando se cumplen todas sus condiciones
 * (mínimo de noches, tipo de habitación, canal y fecha de llegada dentro de la vigencia); se aplica una por reserva.
 */
@Entity
@Table(name = "promociones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    /** Noches mínimas de la estadía (null o 0: sin mínimo). */
    @Column(name = "minimo_noches")
    private Integer minimoNoches;

    /** Solo para este tipo de habitación (null: cualquiera). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_habitacion", length = 20)
    private TipoHabitacion tipoHabitacion;

    /** Solo para este canal de reserva (null: cualquiera). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CanalReserva canal;

    /** La fecha de llegada debe estar dentro de la vigencia (extremos opcionales). */
    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private TipoDescuento tipoDescuento;

    /** Valor en pesos (VALOR_FIJO) o porcentaje (PORCENTAJE). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Builder.Default
    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creada_por", length = 50)
    private String creadaPor;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;

    public boolean aplica(int noches, TipoHabitacion tipo, CanalReserva canalReserva, LocalDate llegada) {
        return activa
                && (minimoNoches == null || noches >= minimoNoches)
                && (tipoHabitacion == null || tipoHabitacion == tipo)
                && (canal == null || canal == canalReserva)
                && (vigenteDesde == null || !llegada.isBefore(vigenteDesde))
                && (vigenteHasta == null || !llegada.isAfter(vigenteHasta));
    }

    /** Descuento sobre el subtotal del alojamiento; nunca supera el subtotal. */
    public BigDecimal descuentoSobre(BigDecimal subtotal) {
        BigDecimal d = tipoDescuento == TipoDescuento.PORCENTAJE
                ? subtotal.multiply(valor).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : valor;
        return d.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    public enum TipoDescuento {
        VALOR_FIJO,
        PORCENTAJE
    }
}
