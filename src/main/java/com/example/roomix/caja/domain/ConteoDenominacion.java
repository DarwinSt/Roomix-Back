package com.example.roomix.caja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cantidad de billetes o monedas de una denominación contados al cerrar el turno. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConteoDenominacion {

    @Column(nullable = false)
    private Integer denominacion;

    @Column(nullable = false)
    private Integer cantidad;
}
