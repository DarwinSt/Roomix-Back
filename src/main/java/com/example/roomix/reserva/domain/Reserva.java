package com.example.roomix.reserva.domain;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.huesped.domain.Huesped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Control de concurrencia: si dos personas guardan el mismo registro a la vez, la segunda recibe un aviso (409). */
    @jakarta.persistence.Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0 not null")
    private long version;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "huesped_id", nullable = false)
    private Huesped huesped;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "cantidad_noches", nullable = false)
    private Integer cantidadNoches;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reserva", nullable = false, length = 20)
    private EstadoReserva estadoReserva;

    @Column(name = "tarifa_noche", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaNoche;

    /** Número de personas de la estadía (null en reservas anteriores: 1). */
    @Column(name = "cantidad_personas")
    private Integer cantidadPersonas;

    /** Modo de cobro vigente al crear la reserva: si es true, la tarifa es por persona y noche. */
    @Column(name = "cobro_por_persona")
    private Boolean cobroPorPersona;

    public int personas() {
        return cantidadPersonas == null || cantidadPersonas < 1 ? 1 : cantidadPersonas;
    }

    public boolean porPersona() {
        return Boolean.TRUE.equals(cobroPorPersona);
    }

    /** Unidades cobradas: noches, o noches × personas cuando se cobra por persona. */
    public int unidadesCobro() {
        return cantidadNoches * (porPersona() ? personas() : 1);
    }

    /** Tarifa × unidades, antes del descuento. */
    public BigDecimal subtotal() {
        return tarifaNoche.multiply(BigDecimal.valueOf(unidadesCobro()));
    }

    @Column(name = "total_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEstimado;

    @Column(name = "hora_real_check_in")
    private LocalDateTime horaRealCheckIn;

    @Column(name = "hora_real_check_out")
    private LocalDateTime horaRealCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'PRESENCIAL'")
    @Builder.Default
    private CanalReserva canal = CanalReserva.PRESENCIAL;

    /** Promoción aplicada (una por reserva); el total estimado ya descuenta su valor. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "promocion_id")
    private Promocion promocion;

    @Column(precision = 12, scale = 2)
    private BigDecimal descuento;

    /** Al cancelar con abonos: parte retenida como penalidad y parte devuelta al huésped. */
    @Column(name = "valor_penalidad", precision = 12, scale = 2)
    private BigDecimal valorPenalidad;

    @Column(name = "valor_devuelto", precision = 12, scale = 2)
    private BigDecimal valorDevuelto;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Column(name = "fecha_hora_cancelacion")
    private LocalDateTime fechaHoraCancelacion;

    @Column(name = "registrada_por", length = 50)
    private String registradaPor;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;
}
