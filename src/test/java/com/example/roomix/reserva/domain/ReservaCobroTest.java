package com.example.roomix.reserva.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Cálculo del cobro de una reserva: por habitación o por persona, y personas por defecto. */
class ReservaCobroTest {

    private static Reserva reserva(int noches, Integer personas, Boolean porPersona) {
        return Reserva.builder()
                .tarifaNoche(new BigDecimal("185000"))
                .cantidadNoches(noches)
                .cantidadPersonas(personas)
                .cobroPorPersona(porPersona)
                .build();
    }

    @Test
    void porHabitacionLasPersonasNoCambianElTotal() {
        Reserva r = reserva(2, 3, false);
        assertThat(r.unidadesCobro()).isEqualTo(2);
        assertThat(r.subtotal()).isEqualByComparingTo("370000");
    }

    @Test
    void porPersonaElTotalEsTarifaPorNochesPorPersonas() {
        Reserva r = reserva(1, 3, true);
        assertThat(r.unidadesCobro()).isEqualTo(3);
        assertThat(r.subtotal()).isEqualByComparingTo("555000");
    }

    @Test
    void sinPersonasIndicadasSeCuentaUna() {
        assertThat(reserva(2, null, true).personas()).isEqualTo(1);
        assertThat(reserva(2, 0, true).subtotal()).isEqualByComparingTo("370000");
    }

    @Test
    void lasReservasAnterioresAlCobroPorPersonaSeCobranPorHabitacion() {
        assertThat(reserva(2, 4, null).porPersona()).isFalse();
        assertThat(reserva(2, 4, null).subtotal()).isEqualByComparingTo("370000");
    }
}
