package com.example.roomix.finanzas.service;

import com.example.roomix.config.HotelProperties;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Exención de IVA para extranjeros no residentes (art. 481 E.T.) y hotel que no cobra IVA. */
class ReglaIvaTest {

    private static HotelProperties hotel(String iva, boolean exentoExtranjeros) {
        return new HotelProperties("Hotel", "900", "Calle", "Ciudad", "300", "h@h.test", "FV", new BigDecimal(iva), exentoExtranjeros);
    }

    private static Huesped huesped(TipoDocumento tipo, String nacionalidad) {
        return Huesped.builder().tipoDocumento(tipo).nacionalidad(nacionalidad).build();
    }

    @Test
    void elExtranjeroConPasaporteQuedaExento() {
        assertThat(ReglaIva.exento(hotel("19", true), huesped(TipoDocumento.PASAPORTE, "Canadá"))).isTrue();
    }

    @Test
    void elColombianoOElQueNoTraePasaporteNoQuedaExento() {
        assertThat(ReglaIva.exento(hotel("19", true), huesped(TipoDocumento.PASAPORTE, "Colombia"))).isFalse();
        assertThat(ReglaIva.exento(hotel("19", true), huesped(TipoDocumento.CEDULA, "Canadá"))).isFalse();
        assertThat(ReglaIva.exento(hotel("19", true), huesped(TipoDocumento.PASAPORTE, ""))).isFalse();
    }

    @Test
    void siElHotelNoAplicaLaExencionNadieQuedaExento() {
        assertThat(ReglaIva.exento(hotel("19", false), huesped(TipoDocumento.PASAPORTE, "Canadá"))).isFalse();
        assertThat(ReglaIva.exento(hotel("19", true), null)).isFalse();
    }
}
