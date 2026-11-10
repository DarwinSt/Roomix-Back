package com.example.roomix.reserva.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.reserva.domain.CanalReserva;
import com.example.roomix.reserva.dto.ActualizarReservaRequest;
import org.springframework.util.StringUtils;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.habitacion.exception.HabitacionNoDisponibleException;
import com.example.roomix.habitacion.exception.HabitacionNotFoundException;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.habitacion.service.HabitacionInhabilitacionService;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.exception.HuespedErrorCode;
import com.example.roomix.huesped.exception.HuespedException;
import com.example.roomix.huesped.service.HuespedService;
import com.example.roomix.reserva.domain.Promocion;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.domain.ReservaCanceladaEvent;
import com.example.roomix.reserva.dto.CrearReservaRequest;
import com.example.roomix.reserva.dto.HabitacionDisponibilidadCheckResponse;
import com.example.roomix.reserva.dto.HabitacionDisponibleResponse;
import com.example.roomix.reserva.dto.ReservaResponse;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.reserva.repository.ReservaSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaService {

    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(
            EstadoReserva.CONFIRMADA,
            EstadoReserva.EN_CURSO
    );

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final HabitacionInhabilitacionService inhabilitacionService;
    private final HuespedService huespedService;
    private final TarifaService tarifaService;
    private final PromocionService promocionService;
    private final ApplicationEventPublisher eventos;

    public List<ReservaResponse> listar(
            EstadoReserva estado,
            Long huespedId,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {
        return reservaRepository.findAll(
                ReservaSpecifications.conFiltros(estado, huespedId, fechaDesde, fechaHasta),
                Sort.by(Sort.Direction.DESC, "fechaEntrada", "id")
        ).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    public ReservaResponse obtenerPorId(Long id) {
        return ReservaResponse.from(buscarEntidad(id));
    }

    public List<ReservaResponse> historialHuesped(Long huespedId) {
        huespedService.buscarActivo(huespedId);
        return reservaRepository.findByHuespedIdOrderByFechaEntradaDesc(huespedId).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    public List<HabitacionDisponibleResponse> consultarDisponibilidad(
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            TipoHabitacion tipoHabitacion
    ) {
        int noches = validarFechas(fechaEntrada, fechaSalida);
        List<Habitacion> habitaciones = tipoHabitacion == null
                ? habitacionRepository.findAll()
                : habitacionRepository.findByTipoHabitacion(tipoHabitacion);

        return habitaciones.stream()
                .filter(h -> estaDisponibleParaReserva(h, fechaEntrada, fechaSalida))
                .map(h -> {
                    BigDecimal tarifa = resolverTarifaNoche(h);
                    return HabitacionDisponibleResponse.builder()
                            .habitacionId(h.getId())
                            .numero(h.getNumero())
                            .tipoHabitacion(h.getTipoHabitacion())
                            .descripcion(h.getDescripcion())
                            .tarifaNoche(tarifa)
                            .cantidadNoches(noches)
                            .totalEstimado(tarifa.multiply(BigDecimal.valueOf(noches)))
                            .build();
                })
                .toList();
    }

    public boolean habitacionDisponibleEnFechas(Long habitacionId, LocalDate fechaEntrada, LocalDate fechaSalida) {
        validarFechas(fechaEntrada, fechaSalida);
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new HabitacionNotFoundException(habitacionId));
        inhabilitacionService.sincronizar(habitacion);
        habitacionRepository.save(habitacion);
        return estaDisponibleParaReserva(habitacion, fechaEntrada, fechaSalida);
    }

    public HabitacionDisponibilidadCheckResponse verificarDisponibilidadHabitacion(
            Long habitacionId,
            LocalDate fechaEntrada,
            LocalDate fechaSalida
    ) {
        int noches = validarFechas(fechaEntrada, fechaSalida);
        Habitacion habitacion = habitacionRepository.findById(habitacionId)
                .orElseThrow(() -> new HabitacionNotFoundException(habitacionId));
        inhabilitacionService.sincronizar(habitacion);
        habitacionRepository.save(habitacion);

        boolean disponible = estaDisponibleParaReserva(habitacion, fechaEntrada, fechaSalida);
        BigDecimal tarifa = resolverTarifaNoche(habitacion);

        return HabitacionDisponibilidadCheckResponse.builder()
                .habitacionId(habitacionId)
                .disponible(disponible)
                .tarifaNoche(tarifa)
                .cantidadNoches(noches)
                .totalEstimado(tarifa.multiply(BigDecimal.valueOf(noches)))
                .build();
    }

    @Transactional
    public ReservaResponse crear(CrearReservaRequest request) {
        Habitacion habitacion = habitacionRepository.findById(request.habitacionId())
                .orElseThrow(() -> new HabitacionNotFoundException(request.habitacionId()));

        inhabilitacionService.sincronizar(habitacion);

        // La disponibilidad se decide por fechas (sin cruces), no por el estado actual de la habitación:
        // así se aceptan reservas anticipadas de habitaciones hoy ocupadas o reservadas.
        Huesped huesped = huespedService.buscarActivo(request.huespedId());
        validarHuespedSinCruce(huesped.getId(), null, request.fechaEntrada(), request.fechaSalida());

        Reserva reserva = registrarReserva(habitacion, huesped, request.fechaEntrada(), request.fechaSalida());
        reserva.setCanal(request.canal() != null ? request.canal() : CanalReserva.PRESENCIAL);
        reserva.setObservaciones(StringUtils.hasText(request.observaciones()) ? request.observaciones().trim() : null);
        if (request.promocionId() != null) {
            aplicarPromocion(reserva, promocionService.validarAplicable(request.promocionId(), habitacion,
                    reserva.getCantidadNoches(), reserva.getCanal(), reserva.getFechaEntrada()));
        }
        reservaRepository.save(reserva);
        sincronizarReservaVigente(habitacion);
        habitacionRepository.save(habitacion);
        return ReservaResponse.from(reserva);
    }

    /**
     * La habitación refleja su reserva vigente: la estadía en curso o, si no la hay, la confirmada que llega primero.
     * Si existe una próxima reserva, la habitación libre pasa a Reservada y la inhabilitada (limpieza o adecuación)
     * vuelve a Reservada al terminar.
     */
    @Transactional
    public void sincronizarReservaVigente(Habitacion habitacion) {
        if (habitacion.getEstado() == EstadoHabitacion.OCUPADO) {
            return;
        }
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaOrderByFechaEntradaAsc(habitacion.getId(), EstadoReserva.CONFIRMADA)
                .ifPresent(proxima -> {
                    aplicarSnapshotEnHabitacion(habitacion, proxima, proxima.getHuesped());
                    if (habitacion.getEstado() == EstadoHabitacion.LIBRE) {
                        habitacion.setEstado(EstadoHabitacion.RESERVADO);
                    } else if (habitacion.getEstado() == EstadoHabitacion.INHABILITADO
                            && habitacion.getEstadoRetorno() != EstadoHabitacion.OCUPADO) {
                        habitacion.setEstadoRetorno(EstadoHabitacion.RESERVADO);
                    }
                });
    }

    @Transactional
    public void marcarEnCurso(Habitacion habitacion) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaOrderByFechaEntradaAsc(habitacion.getId(), EstadoReserva.CONFIRMADA)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.EN_CURSO);
                    reserva.setHoraRealCheckIn(LocalDateTime.now());
                    reservaRepository.save(reserva);
                });
    }

    @Transactional
    public void finalizar(Habitacion habitacion, LocalDateTime horaCheckOut) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaOrderByFechaEntradaAsc(habitacion.getId(), EstadoReserva.EN_CURSO)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.FINALIZADA);
                    reserva.setHoraRealCheckOut(horaCheckOut);
                    reservaRepository.save(reserva);
                });
    }

    @Transactional
    public void cancelar(Habitacion habitacion) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaOrderByFechaEntradaAsc(habitacion.getId(), EstadoReserva.CONFIRMADA)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.CANCELADA);
                    reserva.setFechaHoraCancelacion(LocalDateTime.now());
                    if (reserva.getMotivoCancelacion() == null) {
                        reserva.setMotivoCancelacion("Cancelada desde el módulo de habitaciones");
                    }
                    reservaRepository.save(reserva);
                    eventos.publishEvent(new ReservaCanceladaEvent(reserva.getId()));
                });
    }

    public List<ReservaResponse> historialHabitacion(Long habitacionId) {
        if (!habitacionRepository.existsById(habitacionId)) {
            throw new HabitacionNotFoundException(habitacionId);
        }
        return reservaRepository.findByHabitacionIdOrderByFechaEntradaDesc(habitacionId).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    /**
     * Modifica una reserva. Confirmada: fechas, huésped, canal y observaciones.
     * En curso: solo la fecha de salida (extensión o salida anticipada) y observaciones.
     */
    @Transactional
    public ReservaResponse actualizar(Long id, ActualizarReservaRequest request) {
        Reserva reserva = buscarEntidad(id);
        Habitacion habitacion = reserva.getHabitacion();
        EstadoReserva estado = reserva.getEstadoReserva();
        if (estado != EstadoReserva.CONFIRMADA && estado != EstadoReserva.EN_CURSO) {
            throw new ReservaException(ReservaErrorCode.RESERVA_NO_EDITABLE,
                    "La reserva no se puede modificar en estado " + estado);
        }
        if (estado == EstadoReserva.EN_CURSO && !request.fechaEntrada().equals(reserva.getFechaEntrada())) {
            throw new ReservaException(ReservaErrorCode.RESERVA_NO_EDITABLE,
                    "La estadía ya inició: solo se puede cambiar la fecha de salida");
        }

        LocalDate entrada = request.fechaEntrada();
        LocalDate salida = request.fechaSalida();
        int noches = validarFechas(entrada, salida);
        if (reservaRepository.existeSolapamientoExcluyendo(habitacion.getId(), reserva.getId(), entrada, salida, ESTADOS_ACTIVOS)) {
            throw new ReservaException(ReservaErrorCode.SOLAPAMIENTO);
        }
        LocalDate desdeMantenimiento = estado == EstadoReserva.EN_CURSO ? reserva.getFechaSalida() : entrada;
        if (salida.isAfter(desdeMantenimiento)
                && inhabilitacionService.tieneMantenimientoEnRango(habitacion.getId(), desdeMantenimiento, salida)) {
            throw new HabitacionNoDisponibleException(
                    "Hay mantenimiento programado en la habitación entre el " + desdeMantenimiento + " y el " + salida);
        }

        if (estado == EstadoReserva.CONFIRMADA) {
            if (request.huespedId() != null && !request.huespedId().equals(reserva.getHuesped().getId())) {
                Huesped nuevo = huespedService.buscarActivo(request.huespedId());
                validarHuespedSinCruce(nuevo.getId(), reserva.getId(), entrada, salida);
                reserva.setHuesped(nuevo);
            }
            if (request.canal() != null) {
                reserva.setCanal(request.canal());
            }
            reserva.setFechaEntrada(entrada);
        }
        reserva.setFechaSalida(salida);
        reserva.setCantidadNoches(noches);
        reserva.setTotalEstimado(reserva.getTarifaNoche().multiply(BigDecimal.valueOf(noches)));
        Promocion promocion = reserva.getPromocion();
        if (promocion != null) {
            if (promocion.aplica(noches, habitacion.getTipoHabitacion(), reserva.getCanal(), reserva.getFechaEntrada())) {
                aplicarPromocion(reserva, promocion);
            } else {
                reserva.setPromocion(null);
                reserva.setDescuento(null);
            }
        }
        reserva.setObservaciones(StringUtils.hasText(request.observaciones()) ? request.observaciones().trim() : null);

        Reserva guardada = reservaRepository.save(reserva);
        if (estado == EstadoReserva.EN_CURSO) {
            habitacion.setFechaSalida(salida);
            habitacion.setCantidadNoches(noches);
        } else {
            sincronizarReservaVigente(habitacion);
        }
        habitacionRepository.save(habitacion);
        return ReservaResponse.from(guardada);
    }

    /** Cancela una reserva confirmada (antes del check-in) registrando el motivo, y libera la habitación. */
    @Transactional
    public ReservaResponse cancelarReserva(Long id, String motivo) {
        Reserva reserva = buscarEntidad(id);
        if (reserva.getEstadoReserva() != EstadoReserva.CONFIRMADA) {
            throw new ReservaException(ReservaErrorCode.RESERVA_NO_CANCELABLE,
                    "Solo se cancelan reservas confirmadas (estado actual: " + reserva.getEstadoReserva() + ")");
        }
        if (!StringUtils.hasText(motivo)) {
            throw new ReservaException(ReservaErrorCode.MOTIVO_CANCELACION_REQUERIDO);
        }
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        reserva.setMotivoCancelacion(motivo.trim());
        reserva.setFechaHoraCancelacion(LocalDateTime.now());
        reservaRepository.save(reserva);
        eventos.publishEvent(new ReservaCanceladaEvent(reserva.getId()));

        Habitacion habitacion = reserva.getHabitacion();
        boolean esReservaActual = habitacion.getHuesped() != null
                && habitacion.getHuesped().getId().equals(reserva.getHuesped().getId())
                && reserva.getFechaEntrada().equals(habitacion.getFechaEntrada());
        if (esReservaActual) {
            habitacion.setFechaEntrada(null);
            habitacion.setFechaSalida(null);
            habitacion.setCantidadNoches(null);
            habitacion.setEstadoReserva(null);
            habitacion.setHoraRealCheckIn(null);
            habitacion.setHoraRealCheckOut(null);
            habitacion.setHuesped(null);
            if (habitacion.getEstado() == EstadoHabitacion.RESERVADO) {
                habitacion.setEstado(EstadoHabitacion.LIBRE);
            } else if (habitacion.getEstadoRetorno() == EstadoHabitacion.RESERVADO) {
                habitacion.setEstadoRetorno(EstadoHabitacion.LIBRE);
            }
            sincronizarReservaVigente(habitacion);
            inhabilitacionService.sincronizar(habitacion);
            habitacionRepository.save(habitacion);
        }
        return ReservaResponse.from(reserva);
    }

    public Reserva buscarReserva(Long id) {
        return buscarEntidad(id);
    }

    public void validarSinSolapamiento(Long habitacionId, LocalDate entrada, LocalDate salida) {
        if (reservaRepository.existeSolapamiento(habitacionId, entrada, salida, ESTADOS_ACTIVOS)) {
            throw new ReservaException(ReservaErrorCode.SOLAPAMIENTO);
        }
    }

    public boolean estaDisponibleParaReserva(Habitacion habitacion, LocalDate entrada, LocalDate salida) {
        inhabilitacionService.sincronizar(habitacion);

        if (!entrada.isAfter(LocalDate.now()) && inhabilitacionService.tieneLimpiezaPendiente(habitacion.getId())) {
            return false;
        }
        if (inhabilitacionService.tieneMantenimientoEnRango(habitacion.getId(), entrada, salida)) {
            return false;
        }
        return !reservaRepository.existeSolapamiento(
                habitacion.getId(), entrada, salida, ESTADOS_ACTIVOS
        );
    }

    private Reserva registrarReserva(
            Habitacion habitacion,
            Huesped huesped,
            LocalDate fechaEntrada,
            LocalDate fechaSalida
    ) {
        int noches = validarFechas(fechaEntrada, fechaSalida);
        validarDisponibilidadOperativa(habitacion, fechaEntrada, fechaSalida);
        validarSinSolapamiento(habitacion.getId(), fechaEntrada, fechaSalida);

        BigDecimal tarifaNoche = resolverTarifaNoche(habitacion);
        BigDecimal total = tarifaNoche.multiply(BigDecimal.valueOf(noches));

        Reserva reserva = Reserva.builder()
                .habitacion(habitacion)
                .huesped(huesped)
                .fechaEntrada(fechaEntrada)
                .fechaSalida(fechaSalida)
                .cantidadNoches(noches)
                .estadoReserva(EstadoReserva.CONFIRMADA)
                .tarifaNoche(tarifaNoche)
                .totalEstimado(total)
                .registradaPor(UsuarioActual.username())
                .build();

        return reservaRepository.save(reserva);
    }

    /** Total = noches × tarifa − descuento de la promoción (que nunca supera el subtotal). */
    private void aplicarPromocion(Reserva reserva, Promocion promocion) {
        BigDecimal subtotal = reserva.getTarifaNoche().multiply(BigDecimal.valueOf(reserva.getCantidadNoches()));
        BigDecimal descuento = promocion.descuentoSobre(subtotal);
        reserva.setPromocion(promocion);
        reserva.setDescuento(descuento);
        reserva.setTotalEstimado(subtotal.subtract(descuento));
    }

    private void validarHuespedSinCruce(Long huespedId, Long reservaId, LocalDate entrada, LocalDate salida) {
        if (reservaRepository.existeSolapamientoHuesped(huespedId, reservaId, entrada, salida, ESTADOS_ACTIVOS)) {
            throw new ReservaException(ReservaErrorCode.HUESPED_CON_RESERVA_EN_FECHAS);
        }
    }

    private void validarDisponibilidadOperativa(Habitacion habitacion, LocalDate entrada, LocalDate salida) {
        // La limpieza posterior a un check-out solo impide las llegadas de hoy (o atrasadas)
        if (!entrada.isAfter(LocalDate.now()) && inhabilitacionService.tieneLimpiezaPendiente(habitacion.getId())) {
            throw new HabitacionNoDisponibleException(
                    "La habitación tiene limpieza pendiente tras el último check-out"
            );
        }
        if (inhabilitacionService.tieneMantenimientoEnRango(habitacion.getId(), entrada, salida)) {
            throw new HabitacionNoDisponibleException(
                    "No se puede reservar: hay mantenimiento programado entre el "
                            + entrada + " y el " + salida.minusDays(1)
                            + " (noches de la estadía)"
            );
        }
    }

    private void aplicarSnapshotEnHabitacion(Habitacion habitacion, Reserva reserva, Huesped huesped) {
        habitacion.setFechaEntrada(reserva.getFechaEntrada());
        habitacion.setFechaSalida(reserva.getFechaSalida());
        habitacion.setCantidadNoches(reserva.getCantidadNoches());
        habitacion.setEstadoReserva(reserva.getEstadoReserva());
        habitacion.setHoraRealCheckIn(null);
        habitacion.setHoraRealCheckOut(null);
        habitacion.setHuesped(huesped);
    }

    private int validarFechas(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new ReservaException(ReservaErrorCode.FECHAS_INVALIDAS);
        }
        int noches = (int) ChronoUnit.DAYS.between(entrada, salida);
        if (noches < 1) {
            throw new ReservaException(ReservaErrorCode.FECHAS_INVALIDAS);
        }
        return noches;
    }

    private Reserva buscarEntidad(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaException(ReservaErrorCode.RESERVA_NO_ENCONTRADA));
    }

    private BigDecimal resolverTarifaNoche(Habitacion habitacion) {
        if (habitacion.getPrecioNoche() != null) {
            return habitacion.getPrecioNoche();
        }
        return tarifaService.obtenerPrecioNoche(habitacion.getTipoHabitacion());
    }
}
