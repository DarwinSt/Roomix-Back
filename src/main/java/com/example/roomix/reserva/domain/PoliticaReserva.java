package com.example.roomix.reserva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Políticas de reserva definidas por el administrador (registro único, id = 1):
 * abono mínimo para confirmar una reserva y penalidad que se retiene del abono al cancelar.
 */
@Entity
@Table(name = "politica_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaReserva {

    public static final long ID = 1L;

    @Id
    private Long id;

    /** Porcentaje del total de la reserva (con impuestos) que se debe abonar para crearla; 0 desactiva el requisito. */
    @Builder.Default
    @Column(name = "abono_minimo_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal abonoMinimoPct = BigDecimal.ZERO;

    /** Porcentaje de lo abonado que se retiene como penalidad cuando se cancela la reserva; el resto se devuelve. */
    @Builder.Default
    @Column(name = "penalidad_cancelacion_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal penalidadCancelacionPct = BigDecimal.ZERO;

    @Column(name = "actualizada_por", length = 50)
    private String actualizadaPor;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion")
    private LocalDateTime fechaHoraUltimaActualizacion;
}
