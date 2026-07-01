package com.example.roomix.habitacion.service;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.MotivoInhabilitacion;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.domain.TipoIncidencia;
import com.example.roomix.incidencia.repository.IncidenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Sincroniza INHABILITADO por mantenimiento del día actual o limpieza post check-out pendiente.
 */
@Service
@RequiredArgsConstructor
public class HabitacionInhabilitacionService {

    private static final List<EstadoIncidencia> ESTADOS_CERRADOS = List.of(
            EstadoIncidencia.FINALIZADA,
            EstadoIncidencia.CANCELADA
    );

    private final HabitacionRepository habitacionRepository;
    private final IncidenciaRepository incidenciaRepository;

    @Transactional
    public void sincronizar(Habitacion habitacion) {
        boolean mantenimientoHoy = tieneMantenimientoHoy(habitacion.getId());
        boolean limpiezaPendiente = tieneLimpiezaPendiente(habitacion.getId());

        if (mantenimientoHoy || limpiezaPendiente) {
            if (habitacion.getEstado() == EstadoHabitacion.RESERVADO && !reservaIncluyeHoy(habitacion)) {
                return;
            }
            if (habitacion.getEstado() != EstadoHabitacion.INHABILITADO) {
                habitacion.setEstadoRetorno(habitacion.getEstado());
                habitacion.setEstado(EstadoHabitacion.INHABILITADO);
            }
            habitacion.setMotivoInhabilitacion(
                    limpiezaPendiente && !mantenimientoHoy
                            ? MotivoInhabilitacion.POST_CHECKOUT
                            : MotivoInhabilitacion.ADECUACION_PROGRAMADA
            );
            return;
        }

        if (habitacion.getEstado() == EstadoHabitacion.INHABILITADO) {
            EstadoHabitacion restaurar = habitacion.getEstadoRetorno() != null
                    ? habitacion.getEstadoRetorno()
                    : EstadoHabitacion.LIBRE;
            habitacion.setEstado(restaurar);
            habitacion.setEstadoRetorno(null);
            habitacion.setMotivoInhabilitacion(null);
        }
    }

    @Transactional
    public void sincronizarTodas() {
        List<Habitacion> habitaciones = habitacionRepository.findAll();
        for (Habitacion habitacion : habitaciones) {
            sincronizar(habitacion);
        }
        habitacionRepository.saveAll(habitaciones);
    }

    public boolean tieneLimpiezaPendiente(Long habitacionId) {
        return incidenciaRepository.existsLimpiezaActiva(
                habitacionId,
                TipoIncidencia.LIMPIEZA,
                ESTADOS_CERRADOS
        );
    }

    public boolean tieneMantenimientoEnRango(Long habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
        if (fechaEntrada == null || fechaSalida == null || !fechaSalida.isAfter(fechaEntrada)) {
            return false;
        }
        LocalDateTime inicio = fechaEntrada.atStartOfDay();
        LocalDateTime fin = fechaSalida.atStartOfDay();
        return incidenciaRepository.tieneMantenimientoActivoEnRango(
                habitacionId,
                inicio,
                fin,
                TipoIncidencia.MANTENIMIENTO,
                ESTADOS_CERRADOS
        );
    }

    private boolean tieneMantenimientoHoy(Long habitacionId) {
        LocalDate hoy = LocalDate.now();
        return incidenciaRepository.tieneMantenimientoActivoProgramadoPara(
                habitacionId,
                hoy.atStartOfDay(),
                hoy.plusDays(1).atStartOfDay(),
                TipoIncidencia.MANTENIMIENTO,
                ESTADOS_CERRADOS
        );
    }

    private boolean reservaIncluyeHoy(Habitacion habitacion) {
        if (habitacion.getFechaEntrada() == null || habitacion.getFechaSalida() == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(habitacion.getFechaEntrada()) && hoy.isBefore(habitacion.getFechaSalida());
    }
}
