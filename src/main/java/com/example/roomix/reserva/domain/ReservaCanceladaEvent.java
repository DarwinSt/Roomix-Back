package com.example.roomix.reserva.domain;

/**
 * Evento de dominio: una reserva confirmada se canceló. Finanzas lo escucha para devolver los abonos
 * aplicando la penalidad, sin que el módulo de reservas dependa del financiero.
 */
public record ReservaCanceladaEvent(Long reservaId) {
}
