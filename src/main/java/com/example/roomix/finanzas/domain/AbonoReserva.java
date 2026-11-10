package com.example.roomix.finanzas.domain;

import com.example.roomix.reserva.domain.Reserva;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Abono (anticipo) recibido sobre una reserva antes de facturarla. Se registra como ingreso el día en que se recibe;
 * en el check-out se aplica como pago de la factura y, si la reserva se cancela, se devuelve descontando la penalidad.
 */
@Entity
@Table(name = "abonos_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(length = 80)
    private String referencia;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "registrado_por", nullable = false, length = 50)
    private String registradoPor;

    /** Factura en la que se aplicó como pago (null: aún no facturado). */
    @Column(name = "factura_id")
    private Long facturaId;

    @Builder.Default
    @Column(nullable = false)
    private boolean devuelto = false;

    @Column(name = "valor_devuelto", precision = 14, scale = 2)
    private BigDecimal valorDevuelto;

    @Column(name = "fecha_hora_devolucion")
    private LocalDateTime fechaHoraDevolucion;

    public boolean vigente() {
        return !devuelto && facturaId == null;
    }
}
