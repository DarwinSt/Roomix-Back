package com.example.roomix.caja.domain;

import com.example.roomix.finanzas.domain.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Totales de un medio de pago guardados al cerrar el turno (el cierre no cambia aunque luego se anule algo). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TotalMedioPago {

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ingresos;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal egresos;
}
