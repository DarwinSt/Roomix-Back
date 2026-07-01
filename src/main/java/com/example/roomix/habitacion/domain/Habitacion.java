package com.example.roomix.habitacion.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "habitaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_habitacion_numero", columnNames = "numero")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número visible en el hotel (ej. 101, 205A). */
    @Column(nullable = false, length = 20)
    private String numero;

    @ElementCollection
    @CollectionTable(
            name = "habitacion_caracteristicas",
            joinColumns = @JoinColumn(name = "habitacion_id")
    )
    @Column(name = "caracteristica", nullable = false, length = 100)
    @Builder.Default
    private List<String> caracteristicas = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_habitacion", nullable = false, length = 30)
    private TipoHabitacion tipoHabitacion;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoHabitacion estado = EstadoHabitacion.LIBRE;

    /** Fecha planificada de entrada (reserva). No cambia con el check-in real. */
    @Column(name = "fecha_entrada")
    private LocalDate fechaEntrada;

    /** Fecha planificada de salida (reserva). No cambia con el check-out real. */
    @Column(name = "fecha_salida")
    private LocalDate fechaSalida;

    /** Noches reservadas (fechaSalida − fechaEntrada). */
    @Column(name = "cantidad_noches")
    private Integer cantidadNoches;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reserva", length = 20)
    private EstadoReserva estadoReserva;

    /** Momento real en que el huésped hizo check-in. */
    @Column(name = "hora_real_check_in")
    private LocalDateTime horaRealCheckIn;

    /** Momento real en que el huésped hizo check-out. */
    @Column(name = "hora_real_check_out")
    private LocalDateTime horaRealCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_inhabilitacion", length = 30)
    private MotivoInhabilitacion motivoInhabilitacion;

    /** Estado operativo guardado mientras la habitación está inhabilitada por mantenimiento. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_retorno", length = 30)
    private EstadoHabitacion estadoRetorno;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;
}
