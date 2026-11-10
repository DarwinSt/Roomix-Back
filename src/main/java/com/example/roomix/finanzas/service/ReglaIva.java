package com.example.roomix.finanzas.service;

import com.example.roomix.config.HotelProperties;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regla del IVA del alojamiento, compartida por la facturación y los abonos. */
final class ReglaIva {

    private ReglaIva() {
    }

    /** Turista extranjero no residente (pasaporte y nacionalidad distinta a Colombia). */
    static boolean exento(HotelProperties hotel, Huesped huesped) {
        if (!hotel.exentoIvaExtranjeros() || huesped == null) {
            return false;
        }
        String nacionalidad = huesped.getNacionalidad() == null ? "" : huesped.getNacionalidad().trim();
        return huesped.getTipoDocumento() == TipoDocumento.PASAPORTE
                && !nacionalidad.isEmpty()
                && !nacionalidad.equalsIgnoreCase("Colombia");
    }

    static BigDecimal porcentaje(HotelProperties hotel, Huesped huesped) {
        return exento(hotel, huesped) ? BigDecimal.ZERO : hotel.ivaPorcentaje();
    }

    static BigDecimal impuesto(BigDecimal subtotal, BigDecimal porcentaje) {
        return subtotal.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
