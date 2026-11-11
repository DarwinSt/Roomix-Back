package com.example.roomix.caja.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.caja.domain.EstadoTurno;
import com.example.roomix.caja.domain.TurnoCaja;
import com.example.roomix.caja.exception.CajaErrorCode;
import com.example.roomix.caja.exception.CajaException;
import com.example.roomix.caja.repository.TurnoCajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decide a qué turno de caja pertenece cada movimiento de dinero (pagos de facturas, abonos, devoluciones, ingresos y
 * egresos). Se usa desde Finanzas, de modo que todo pago que registra el cajero queda en su turno sin pasos extra.
 * La recepción no puede mover dinero sin un turno abierto a su nombre; los demás roles (administración, finanzas)
 * pueden registrar pagos fuera de la caja, por ejemplo transferencias recibidas en la oficina.
 */
@Component
@RequiredArgsConstructor
public class CajaTurnoResolver {

    private final TurnoCajaRepository turnos;

    /** Id del turno abierto del usuario actual, null si no aplica; lanza error si es recepción sin turno. */
    public Long turnoParaMovimiento() {
        String usuario = UsuarioActual.username();
        if (UsuarioActual.SISTEMA.equals(usuario)) {
            return null;
        }
        Optional<TurnoCaja> abierto = turnos.findFirstByEstado(EstadoTurno.ABIERTO);
        if (abierto.isPresent() && abierto.get().getCajero().equalsIgnoreCase(usuario)) {
            return abierto.get().getId();
        }
        if (UsuarioActual.tieneRol("RECEPCION")) {
            if (abierto.isPresent()) {
                throw new CajaException(CajaErrorCode.CAJA_DE_OTRO, abierto.get().getCajeroNombre());
            }
            throw new CajaException(CajaErrorCode.TURNO_REQUERIDO);
        }
        return null;
    }

    /** Un movimiento de un turno ya cerrado y entregado no se anula: alteraría un arqueo firmado. */
    public void validarAnulable(Long turnoId) {
        if (turnoId == null) {
            return;
        }
        turnos.findById(turnoId).filter(t -> !t.abierto()).ifPresent(t -> {
            throw new CajaException(CajaErrorCode.MOVIMIENTO_DE_TURNO_CERRADO, "#" + t.getId());
        });
    }
}
