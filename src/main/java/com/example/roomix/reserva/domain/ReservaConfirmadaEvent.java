package com.example.roomix.reserva.domain;

/** Se publica al crear una reserva (con su abono inicial registrado). */
public record ReservaConfirmadaEvent(Long reservaId) {
}
