package com.example.roomix.evento.domain;

import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoAcceso;
import com.example.roomix.evento.domain.EventoEnums.TipoEvento;
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
import jakarta.persistence.Index;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos", indexes = @Index(name = "idx_evento_inicio", columnList = "fecha_hora_inicio"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 600)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acceso", nullable = false, length = 20)
    private TipoAcceso tipoAcceso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoEvento estado = EstadoEvento.PROGRAMADO;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    private Personal responsable;

    /** Máximo de participantes (no puede superar la capacidad del salón). */
    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "organizador_nombre", nullable = false, length = 150)
    private String organizadorNombre;

    @Column(name = "organizador_documento", length = 40)
    private String organizadorDocumento;

    @Column(name = "organizador_telefono", length = 30)
    private String organizadorTelefono;

    @Column(name = "organizador_email", length = 120)
    private String organizadorEmail;

    /** Valor acordado por el uso del espacio / paquete base. */
    @Column(name = "valor_base", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorBase;

    @Column(length = 500)
    private String notas;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Column(name = "creado_por", nullable = false, length = 50)
    private String creadoPor;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<EventoRecurso> recursos = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nombre ASC")
    @Builder.Default
    private List<EventoParticipante> participantes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;

    public BigDecimal totalRecursos() {
        return recursos.stream().map(EventoRecurso::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal valorTotal() {
        return valorBase.add(totalRecursos());
    }
}
