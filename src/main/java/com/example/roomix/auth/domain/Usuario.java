package com.example.roomix.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false, length = 120)
    private String email;

    /** Hash BCrypt de la contraseña; la contraseña en claro nunca se almacena. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Rol principal (se muestra en la auditoría). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Permisos marcados para este usuario (solo cuentan si permisosPersonalizados es true). */
    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(name = "usuario_permisos", joinColumns = @jakarta.persistence.JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", length = 40)
    @Builder.Default
    private java.util.Set<Permiso> permisos = new java.util.LinkedHashSet<>();

    /** false o null: el usuario tiene los permisos por defecto de su rol. */
    @Column(name = "permisos_personalizados")
    private Boolean permisosPersonalizados;

    /** Permisos con los que trabaja el usuario: los marcados o, si no se personalizaron, los de su rol. */
    public java.util.Set<Permiso> permisosEfectivos() {
        if (Boolean.TRUE.equals(permisosPersonalizados)) {
            java.util.EnumSet<Permiso> set = java.util.EnumSet.noneOf(Permiso.class);
            if (permisos != null) {
                set.addAll(permisos);
            }
            return set;
        }
        return Permiso.porDefecto(rol);
    }

    public boolean tienePermiso(Permiso p) {
        return permisosEfectivos().contains(p);
    }

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Obliga a cambiar la contraseña en el próximo ingreso (contraseña temporal). */
    @Column(name = "debe_cambiar_password", nullable = false)
    @Builder.Default
    private boolean debeCambiarPassword = false;

    /** Intentos fallidos consecutivos; al llegar al límite la cuenta se bloquea temporalmente. */
    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @CreationTimestamp
    @Column(name = "fecha_hora_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaHoraCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }
}
