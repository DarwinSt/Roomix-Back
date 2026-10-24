package com.example.roomix.reserva.domain;

/**
 * Canal por el que llegó la reserva (presencial o en línea), registrado por recepción.
 */
public enum CanalReserva {
    PRESENCIAL("Presencial"),
    TELEFONO("Teléfono"),
    CORREO("Correo electrónico"),
    WHATSAPP("WhatsApp"),
    SITIO_WEB("Sitio web del hotel"),
    AGENCIA("Agencia / OTA");

    private final String nombre;

    CanalReserva(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
