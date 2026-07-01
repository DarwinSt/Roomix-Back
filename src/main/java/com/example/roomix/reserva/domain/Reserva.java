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

    @Column(name = "total_estimado", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEstimado;

    @Column(name = "hora_real_check_in")
    private LocalDateTime horaRealCheckIn;

    @Column(name = "hora_real_check_out")
    private LocalDateTime horaRealCheckOut;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;
}
