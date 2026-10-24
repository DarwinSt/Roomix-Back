package com.example.roomix.evento.domain;

import com.example.roomix.huesped.domain.Huesped;
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

import java.time.LocalDateTime;

/** Inscripción a un evento; puede corresponder a un huésped del hotel. */
@Entity
@Table(name = "evento_participantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "huesped_id")
    private Huesped huesped;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 40)
    private String documento;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private boolean confirmado = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean asistio = false;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;
}
