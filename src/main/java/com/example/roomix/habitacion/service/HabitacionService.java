package com.example.roomix.habitacion.service;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.HabitacionEstadoTransicion;
import com.example.roomix.habitacion.domain.MotivoInhabilitacion;
import com.example.roomix.habitacion.dto.ActualizarEstadoRequest;
import com.example.roomix.habitacion.dto.HabitacionRequest;
import com.example.roomix.habitacion.dto.HabitacionResponse;
import com.example.roomix.habitacion.exception.HabitacionNoDisponibleException;
import com.example.roomix.habitacion.exception.HabitacionNotFoundException;
import com.example.roomix.habitacion.exception.NumeroHabitacionDuplicadoException;
import com.example.roomix.habitacion.exception.ReservaInvalidaException;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.exception.HuespedErrorCode;
import com.example.roomix.huesped.exception.HuespedException;
import com.example.roomix.huesped.service.HuespedService;
import com.example.roomix.incidencia.repository.IncidenciaRepository;
import com.example.roomix.incidencia.service.IncidenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitacionService {

    private final HabitacionRepository habitacionRepository;
    private final HabitacionInhabilitacionService inhabilitacionService;
    private final IncidenciaRepository incidenciaRepository;
    private final HuespedService huespedService;
    @Lazy
    private final IncidenciaService incidenciaService;

    public List<HabitacionResponse> listar(EstadoHabitacion estado) {
        List<Habitacion> habitaciones = estado == null
                ? habitacionRepository.findAll()
                : habitacionRepository.findByEstado(estado);

        for (Habitacion habitacion : habitaciones) {
            inhabilitacionService.sincronizar(habitacion);
        }
        habitacionRepository.saveAll(habitaciones);

        return habitaciones.stream().map(HabitacionResponse::from).toList();
    }

    public HabitacionResponse obtenerPorId(Long id) {
        Habitacion habitacion = buscarEntidad(id);
        inhabilitacionService.sincronizar(habitacion);
        return HabitacionResponse.from(habitacionRepository.save(habitacion));
    }

    @Transactional
    public HabitacionResponse crear(HabitacionRequest request) {
        validarNumeroUnico(request.numero(), null);

        EstadoHabitacion estado = request.estado() != null ? request.estado() : EstadoHabitacion.LIBRE;

        Habitacion habitacion = Habitacion.builder()
                .numero(request.numero().trim())
                .caracteristicas(normalizarCaracteristicas(request.caracteristicas()))
                .tipoHabitacion(request.tipoHabitacion())
                .descripcion(request.descripcion().trim())
                .estado(estado)
                .build();

        if (estado != EstadoHabitacion.LIBRE) {
            HabitacionEstadoTransicion.validar(EstadoHabitacion.LIBRE, estado, null);
        }
        if (estado == EstadoHabitacion.RESERVADO) {
            validarDisponibleParaReserva(habitacion, request.fechaEntrada(), request.fechaSalida());
            aplicarNuevaReserva(habitacion, request.fechaEntrada(), request.fechaSalida(), request.huespedId());
        }

        return HabitacionResponse.from(habitacionRepository.save(habitacion));
    }

    @Transactional
    public HabitacionResponse actualizar(Long id, HabitacionRequest request) {
        Habitacion habitacion = buscarEntidad(id);
        validarNumeroUnico(request.numero(), id);

        habitacion.setNumero(request.numero().trim());
        habitacion.getCaracteristicas().clear();
        habitacion.getCaracteristicas().addAll(normalizarCaracteristicas(request.caracteristicas()));
        habitacion.setTipoHabitacion(request.tipoHabitacion());
        habitacion.setDescripcion(request.descripcion().trim());

        if (request.estado() != null) {
            inhabilitacionService.sincronizar(habitacion);
            EstadoHabitacion estadoAnterior = habitacion.getEstado();
            if (request.estado() == EstadoHabitacion.RESERVADO) {
                validarDisponibleParaReserva(habitacion, request.fechaEntrada(), request.fechaSalida());
            }
            HabitacionEstadoTransicion.validar(
                    estadoAnterior,
                    request.estado(),
                    null,
                    request.estado() == EstadoHabitacion.RESERVADO
                            && estadoAnterior == EstadoHabitacion.INHABILITADO
            );
            aplicarTransicionReserva(
                    habitacion,
                    estadoAnterior,
                    request.estado(),
                    request.fechaEntrada(),
                    request.fechaSalida(),
                    request.huespedId()
            );
            if (request.estado() == EstadoHabitacion.RESERVADO) {
                habitacion.setEstadoRetorno(null);
                habitacion.setMotivoInhabilitacion(null);
            }
            habitacion.setEstado(request.estado());
        } else if (request.fechaEntrada() != null && request.fechaSalida() != null
                && habitacion.getEstado() == EstadoHabitacion.RESERVADO) {
            validarDisponibleParaReserva(habitacion, request.fechaEntrada(), request.fechaSalida());
            aplicarNuevaReserva(habitacion, request.fechaEntrada(), request.fechaSalida(), request.huespedId());
        }

        inhabilitacionService.sincronizar(habitacion);
        return HabitacionResponse.from(habitacionRepository.save(habitacion));
    }

    @Transactional
    public HabitacionResponse actualizarEstado(Long id, ActualizarEstadoRequest request) {
        Habitacion habitacion = buscarEntidad(id);
        inhabilitacionService.sincronizar(habitacion);

        EstadoHabitacion estadoActual = habitacion.getEstado();
        boolean esCheckOut = estadoActual == EstadoHabitacion.OCUPADO
                && request.estado() == EstadoHabitacion.LIBRE;

        if (request.estado() == EstadoHabitacion.RESERVADO) {
            validarDisponibleParaReserva(habitacion, request.fechaEntrada(), request.fechaSalida());
        }

        if (esCheckOut) {
            aplicarTransicionReserva(
                    habitacion,
                    estadoActual,
                    EstadoHabitacion.LIBRE,
                    request.fechaEntrada(),
                    request.fechaSalida(),
                    null
            );
            habitacion.setEstado(EstadoHabitacion.INHABILITADO);
            habitacion.setMotivoInhabilitacion(MotivoInhabilitacion.POST_CHECKOUT);
            habitacion.setEstadoRetorno(EstadoHabitacion.LIBRE);
            Habitacion guardada = habitacionRepository.save(habitacion);
            incidenciaService.crearLimpiezaPostCheckout(guardada);
            return HabitacionResponse.from(guardada);
        }

        boolean reservaDesdeInhabilitado = estadoActual == EstadoHabitacion.INHABILITADO
                && request.estado() == EstadoHabitacion.RESERVADO;

        HabitacionEstadoTransicion.validar(
                estadoActual,
                request.estado(),
                request.motivoInhabilitacion(),
                reservaDesdeInhabilitado
        );

        aplicarTransicionReserva(
                habitacion,
                estadoActual,
                request.estado(),
                request.fechaEntrada(),
                request.fechaSalida(),
                request.huespedId()
        );

        if (request.estado() == EstadoHabitacion.RESERVADO) {
            habitacion.setEstadoRetorno(null);
            habitacion.setMotivoInhabilitacion(null);
        }

        habitacion.setEstado(request.estado());
        Habitacion guardada = habitacionRepository.save(habitacion);
        inhabilitacionService.sincronizar(guardada);
        return HabitacionResponse.from(habitacionRepository.save(guardada));
    }

    @Transactional
    public void eliminar(Long id) {
        Habitacion habitacion = buscarEntidad(id);
        incidenciaRepository.deleteTareasByHabitacionId(id);
        incidenciaRepository.deleteByHabitacionId(id);
        habitacionRepository.delete(habitacion);
    }

    public void limpiarReserva(Habitacion habitacion) {
        habitacion.setFechaEntrada(null);
        habitacion.setFechaSalida(null);
        habitacion.setCantidadNoches(null);
        habitacion.setEstadoReserva(null);
        habitacion.setHoraRealCheckIn(null);
        habitacion.setHoraRealCheckOut(null);
        habitacion.setHuesped(null);
    }

    public boolean tieneReservaVigente(Habitacion habitacion) {
        return habitacion.getEstadoReserva() == EstadoReserva.CONFIRMADA
                && habitacion.getFechaEntrada() != null;
    }

    private Habitacion buscarEntidad(Long id) {
        return habitacionRepository.findById(id)
                .orElseThrow(() -> new HabitacionNotFoundException(id));
    }

    private void validarDisponibleParaReserva(
            Habitacion habitacion,
            LocalDate fechaEntrada,
            LocalDate fechaSalida
    ) {
        if (inhabilitacionService.tieneLimpiezaPendiente(habitacion.getId())) {
            throw new HabitacionNoDisponibleException(
                    "La habitación tiene limpieza pendiente tras el último check-out"
            );
        }

        LocalDate entrada = fechaEntrada != null ? fechaEntrada : LocalDate.now();
        LocalDate salida = fechaSalida != null ? fechaSalida : entrada.plusDays(1);

        if (!salida.isAfter(entrada)) {
            throw new ReservaInvalidaException("La fecha de salida debe ser posterior a la de entrada");
        }

        if (inhabilitacionService.tieneMantenimientoEnRango(habitacion.getId(), entrada, salida)) {
            throw new HabitacionNoDisponibleException(
                    "No se puede reservar: hay mantenimiento programado entre el "
                            + entrada + " y el " + salida.minusDays(1)
                            + " (noches de la estadía)"
            );
        }
    }

    private void validarNumeroUnico(String numero, Long idExcluido) {
        if (idExcluido == null) {
            habitacionRepository.findByNumero(numero.trim())
                    .ifPresent(h -> {
                        throw new NumeroHabitacionDuplicadoException(numero);
                    });
            return;
        }
        if (habitacionRepository.existsByNumeroAndIdNot(numero.trim(), idExcluido)) {
            throw new NumeroHabitacionDuplicadoException(numero);
        }
    }

    private void aplicarTransicionReserva(
            Habitacion habitacion,
            EstadoHabitacion estadoActual,
            EstadoHabitacion nuevoEstado,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            Long huespedId
    ) {
        if (nuevoEstado == EstadoHabitacion.RESERVADO) {
            aplicarNuevaReserva(habitacion, fechaEntrada, fechaSalida, huespedId);
            return;
        }

        if (estadoActual == EstadoHabitacion.RESERVADO && nuevoEstado == EstadoHabitacion.OCUPADO) {
            if (habitacion.getHuesped() == null) {
                throw new HuespedException(HuespedErrorCode.HUESPED_SIN_ASIGNAR_CHECKIN);
            }
            habitacion.setHoraRealCheckIn(LocalDateTime.now());
            habitacion.setEstadoReserva(EstadoReserva.EN_CURSO);
            return;
        }

        if (estadoActual == EstadoHabitacion.OCUPADO && nuevoEstado == EstadoHabitacion.LIBRE) {
            habitacion.setHoraRealCheckOut(LocalDateTime.now());
            habitacion.setEstadoReserva(EstadoReserva.FINALIZADA);
            limpiarReserva(habitacion);
            return;
        }

        if (estadoActual == EstadoHabitacion.RESERVADO && nuevoEstado == EstadoHabitacion.LIBRE) {
            habitacion.setEstadoReserva(EstadoReserva.CANCELADA);
            limpiarReserva(habitacion);
            return;
        }

        if (nuevoEstado == EstadoHabitacion.LIBRE) {
            limpiarReserva(habitacion);
        }
    }

    private void aplicarNuevaReserva(
            Habitacion habitacion,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            Long huespedId
    ) {
        if (huespedId == null) {
            throw new HuespedException(HuespedErrorCode.HUESPED_REQUERIDO);
        }
        Huesped huesped = huespedService.buscarActivo(huespedId);
        huespedService.validarDisponibleParaAsignar(huesped, habitacion.getId());

        LocalDate entrada = fechaEntrada != null ? fechaEntrada : LocalDate.now();
        LocalDate salida = fechaSalida != null ? fechaSalida : entrada.plusDays(1);

        if (!salida.isAfter(entrada)) {
            throw new ReservaInvalidaException("La fecha de salida debe ser posterior a la de entrada");
        }

        int noches = (int) ChronoUnit.DAYS.between(entrada, salida);
        if (noches < 1) {
            throw new ReservaInvalidaException("La reserva debe incluir al menos 1 noche");
        }

        habitacion.setFechaEntrada(entrada);
        habitacion.setFechaSalida(salida);
        habitacion.setCantidadNoches(noches);
        habitacion.setEstadoReserva(EstadoReserva.CONFIRMADA);
        habitacion.setHoraRealCheckIn(null);
        habitacion.setHoraRealCheckOut(null);
        habitacion.setHuesped(huesped);
    }

    private List<String> normalizarCaracteristicas(List<String> caracteristicas) {
        if (caracteristicas == null || caracteristicas.isEmpty()) {
            return new ArrayList<>();
        }
        return caracteristicas.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }
}
