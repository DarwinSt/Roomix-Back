package com.example.roomix.incidencia.domain;

/**
 * Subtipo de limpieza. Solo aplica cuando {@link TipoIncidencia} es LIMPIEZA.
 * <ul>
 *   <li>POST_CHECKOUT — automática tras check-out; bloquea reservas hasta finalizar.</li>
 *   <li>URGENCIA — huésped presente; situación urgente (daño, derrame, etc.).</li>
 *   <li>HUESPED_AUSENTE — huésped salió temporalmente; reserva sigue activa.</li>
 * </ul>
 */
public enum ContextoLimpieza {
    POST_CHECKOUT,
    URGENCIA,
    HUESPED_AUSENTE
}
