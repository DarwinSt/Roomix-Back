package com.example.roomix.reserva.service;

import com.example.roomix.finanzas.dto.FacturaDtos.FacturaResponse;
import com.example.roomix.finanzas.service.FacturacionService;
import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.dto.ActualizarEstadoRequest;
import com.example.roomix.habitacion.dto.HabitacionResponse;
import com.example.roomix.habitacion.service.HabitacionService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.dto.ReservaResponse;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Check-in y check-out directamente desde la reserva (recepción y dashboard).
 * Reutiliza las transiciones de la habitación para conservar todas sus reglas
 * (limpieza automática tras el check-out, sincronización de estados, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaOperacionService {

    private final ReservaService reservaService;
    private final HabitacionService habitacionService;
    private final FacturacionService facturacionService;

    public HabitacionResponse checkIn(Long reservaId) {
        Reserva reserva = reservaService.buscarReserva(reservaId);
        if (reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            throw new ReservaException(ReservaErrorCode.CHECKIN_NO_PERMITIDO,
                    "Solo se hace check-in de reservas confirmadas (estado actual: " + reserva.getEstadoReserva() + ")");
        }
        if (reserva.getFechaEntrada().isAfter(LocalDate.now())) {
            throw new ReservaException(ReservaErrorCode.CHECKIN_NO_PERMITIDO,
                    "La llegada está prevista para el " + reserva.getFechaEntrada() + ". Modifique la reserva si el huésped llegó antes");
        }
        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion.getEstado() != EstadoHabitacion.RESERVADO) {
            throw new ReservaException(ReservaErrorCode.CHECKIN_NO_PERMITIDO,
                    "La habitación " + habitacion.getNumero() + " está " + habitacion.getEstado().name().toLowerCase()
                            + "; debe estar reservada para registrar el ingreso");
        }
        return habitacionService.actualizarEstado(habitacion.getId(),
                new ActualizarEstadoRequest(EstadoHabitacion.OCUPADO, null, null, null, null));
    }

    public CheckOutResponse checkOut(Long reservaId) {
        Reserva reserva = reservaService.buscarReserva(reservaId);
        if (reserva.getEstadoReserva() != EstadoReserva.EN_CURSO) {
            throw new ReservaException(ReservaErrorCode.CHECKOUT_NO_PERMITIDO,
                    "Solo se hace check-out de estadías en curso (estado actual: " + reserva.getEstadoReserva() + ")");
        }
        Habitacion habitacion = reserva.getHabitacion();
        if (habitacion.getEstado() != EstadoHabitacion.OCUPADO) {
            throw new ReservaException(ReservaErrorCode.CHECKOUT_NO_PERMITIDO,
                    "La habitación " + habitacion.getNumero() + " no figura como ocupada");
        }
        HabitacionResponse habitacionActualizada = habitacionService.actualizarEstado(habitacion.getId(),
                new ActualizarEstadoRequest(EstadoHabitacion.LIBRE, null, null, null, null));

        FacturaResponse factura = null;
        try {
            factura = facturacionService.facturarReserva(reservaId);
        } catch (RuntimeException ex) {
            log.warn("Check-out de la reserva {} sin factura automática: {}", reservaId, ex.getMessage());
        }
        return new CheckOutResponse(habitacionActualizada, reservaService.obtenerPorId(reservaId), factura);
    }

    @Schema(name = "CheckOutResponse")
    public record CheckOutResponse(HabitacionResponse habitacion, ReservaResponse reserva, FacturaResponse factura) {
    }
}
