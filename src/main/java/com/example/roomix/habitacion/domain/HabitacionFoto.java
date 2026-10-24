package com.example.roomix.habitacion.domain;

import jakarta.persistence.Basic;
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

/**
 * Fotografía de una habitación. Se guarda en la base de datos (no en disco) porque las
 * plataformas en la nube (Render, Railway) borran el sistema de archivos en cada despliegue.
 */
@Entity
@Table(name = "habitacion_fotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitacionFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Habitacion habitacion;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] contenido;

    @Column(name = "tipo_contenido", nullable = false, length = 40)
    private String tipoContenido;

    @Column(name = "nombre_archivo", length = 150)
    private String nombreArchivo;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "fecha_hora_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;
}
