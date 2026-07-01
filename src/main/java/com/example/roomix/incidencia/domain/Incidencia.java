package com.example.roomix.incidencia.domain;

import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.personal.domain.Personal;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlcanceIncidencia alcance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habitacion_id")
    private Habitacion habitacion;

    /** Lugar fuera de habitación (ej. Lobby, Piscina, Pasillo 2). */
    @Column(name = "ubicacion", length = 150)
    private String ubicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_id")
    private Personal personalAsignado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoIncidencia tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EstadoIncidencia estado = EstadoIncidencia.CREADA;

    @OneToMany(mappedBy = "incidencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<IncidenciaTarea> tareas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;

    @Column(name = "fecha_hora_finalizacion")
    private LocalDateTime fechaHoraFinalizacion;

    /** Fecha y hora programada para mantenimiento (solo tipo MANTENIMIENTO). */
    @Column(name = "fecha_hora_programada")
    private LocalDateTime fechaHoraProgramada;
}
