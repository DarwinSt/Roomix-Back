package com.example.roomix.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * Registro de auditoría: quién hizo qué, cuándo, desde dónde y con qué resultado.
 */
@Entity
@Table(
        name = "auditoria",
        indexes = {
                @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
                @Index(name = "idx_auditoria_usuario", columnList = "usuario"),
                @Index(name = "idx_auditoria_modulo", columnList = "modulo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(length = 20)
    private String rol;

    /** CREAR, ACTUALIZAR, ELIMINAR, CAMBIO_ESTADO, LOGIN, LOGIN_FALLIDO, ... */
    @Column(nullable = false, length = 30)
    private String accion;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(name = "entidad_id", length = 40)
    private String entidadId;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(length = 200)
    private String ruta;

    @Column(name = "estado_http")
    private Integer estadoHttp;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    @Column(length = 64)
    private String ip;

    @Column(length = 500)
    private String detalle;
}
