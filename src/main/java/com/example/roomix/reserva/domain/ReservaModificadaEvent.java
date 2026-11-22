package com.example.roomix.reserva.domain;

/** Se publica cuando cambian las fechas o las personas de una reserva (para avisar al huésped). */
public record ReservaModificadaEvent(Long reservaId) {
}
