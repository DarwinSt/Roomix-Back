package com.example.roomix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Datos del establecimiento que aparecen en comprobantes y facturas, y reglas de impuestos.
 * Se configuran con variables de entorno (HOTEL_NOMBRE, HOTEL_NIT, ...), así cada hotel
 * adapta el sistema sin modificar código.
 */
@ConfigurationProperties(prefix = "app.hotel")
public record HotelProperties(
        String nombre,
        String nit,
        String direccion,
        String ciudad,
        String telefono,
        String email,
        String prefijoFactura,
        BigDecimal ivaPorcentaje,
        boolean exentoIvaExtranjeros
) {
    public HotelProperties {
        nombre = valor(nombre, "Hotel Roomix");
        nit = valor(nit, "900.000.000-0");
        direccion = valor(direccion, "Calle del Arsenal # 10-25");
        ciudad = valor(ciudad, "Cartagena de Indias, Colombia");
        telefono = valor(telefono, "+57 605 000 0000");
        email = valor(email, "reservas@roomix.test");
        prefijoFactura = valor(prefijoFactura, "FV");
        ivaPorcentaje = ivaPorcentaje != null ? ivaPorcentaje : new BigDecimal("19");
    }

    private static String valor(String v, String porDefecto) {
        return v == null || v.isBlank() ? porDefecto : v;
    }
}
