package com.example.roomix.config;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos y opciones del hotel (registro único, id = 1), editables por el administrador: identidad (nombre, logo, NIT y
 * contacto), impuestos, modo de cobro, retención de datos de huéspedes y correo de confirmación. Al primer arranque se
 * llena con las variables HOTEL_* del servidor.
 */
@Entity
@Table(name = "configuracion_hotel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionHotel {

    public static final long ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 40)
    private String nit;

    @Column(length = 150)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(length = 40)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "prefijo_factura", length = 10)
    private String prefijoFactura;

    /** El hotel cobra IVA sobre el alojamiento y los servicios (false: régimen sin IVA). */
    @Column(name = "cobra_iva")
    private Boolean cobraIva;

    @Column(name = "iva_porcentaje", precision = 5, scale = 2)
    private BigDecimal ivaPorcentaje;

    @Column(name = "exento_iva_extranjeros")
    private Boolean exentoIvaExtranjeros;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_cobro", length = 20)
    private ModoCobro modoCobro;

    /** Meses sin actividad tras los cuales se anonimizan los datos personales del huésped (0 o null: nunca). */
    @Column(name = "meses_retencion_huespedes")
    private Integer mesesRetencionHuespedes;

    @Column(name = "correo_confirmacion_reserva")
    private Boolean correoConfirmacionReserva;

    /** Color principal de la interfaz (#RRGGBB); null = verde de Roomix. */
    @Column(name = "color_primario", length = 7)
    private String colorPrimario;

    /** Color de acento (#RRGGBB); null = dorado de Roomix. */
    @Column(name = "color_acento", length = 7)
    private String colorAcento;

    /** Frase corta del hotel que se muestra en la pantalla de ingreso. */
    @Column(length = 120)
    private String eslogan;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo")
    private byte[] logo;

    @Column(name = "logo_tipo", length = 40)
    private String logoTipo;

    /** Logo como se subió, guardado al quitarle el fondo para poder volver a él. */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo_original")
    private byte[] logoOriginal;

    @Column(name = "logo_original_tipo", length = 40)
    private String logoOriginalTipo;

    /** ORIGINAL, ICONO o AVATAR (null = ORIGINAL). */
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "logo_forma", length = 12)
    private LogoForma logoForma;

    public LogoForma forma() {
        return logoForma != null ? logoForma : LogoForma.ORIGINAL;
    }

    @Column(name = "actualizada_por", length = 50)
    private String actualizadaPor;

    @UpdateTimestamp
    @Column(name = "fecha_hora_actualizacion")
    private LocalDateTime fechaHoraActualizacion;

    public boolean cobraIva() {
        return !Boolean.FALSE.equals(cobraIva);
    }

    public boolean porPersona() {
        return modoCobro == ModoCobro.POR_PERSONA;
    }

    public boolean tieneLogo() {
        return logoTipo != null;
    }
}
