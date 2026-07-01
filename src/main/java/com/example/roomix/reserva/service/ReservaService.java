package com.example.roomix.reserva.service;

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
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.dto.CrearReservaRequest;
import com.example.roomix.reserva.dto.HabitacionDisponibilidadCheckResponse;
import com.example.roomix.reserva.dto.HabitacionDisponibleResponse;
import com.example.roomix.reserva.dto.ReservaResponse;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.reserva.repository.ReservaSpecifications;
import lombok.RequiredArgsConstructor;
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

        if (habitacion.getEstado() != EstadoHabitacion.LIBRE) {
            throw new ReservaException(ReservaErrorCode.HABITACION_NO_LIBRE);
        }

        Huesped huesped = huespedService.buscarActivo(request.huespedId());
        huespedService.validarDisponibleParaAsignar(huesped, habitacion.getId());

        Reserva reserva = registrarReserva(habitacion, huesped, request.fechaEntrada(), request.fechaSalida());
        aplicarSnapshotEnHabitacion(habitacion, reserva, huesped);
        habitacion.setEstado(EstadoHabitacion.RESERVADO);
        habitacionRepository.save(habitacion);
        return ReservaResponse.from(reserva);
    }

    @Transactional
    public Reserva registrarEnHabitacion(
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
        Reserva reserva = registrarReserva(habitacion, huesped, fechaEntrada, fechaSalida);
        aplicarSnapshotEnHabitacion(habitacion, reserva, huesped);
        return reserva;
    }

    @Transactional
    public void marcarEnCurso(Habitacion habitacion) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaIn(habitacion.getId(), ESTADOS_ACTIVOS)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.EN_CURSO);
                    reserva.setHoraRealCheckIn(LocalDateTime.now());
                    reservaRepository.save(reserva);
                });
    }

    @Transactional
    public void finalizar(Habitacion habitacion, LocalDateTime horaCheckOut) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaIn(habitacion.getId(), ESTADOS_ACTIVOS)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.FINALIZADA);
                    reserva.setHoraRealCheckOut(horaCheckOut);
                    reservaRepository.save(reserva);
                });
    }

    @Transactional
    public void cancelar(Habitacion habitacion) {
        reservaRepository.findFirstByHabitacionIdAndEstadoReservaIn(habitacion.getId(), ESTADOS_ACTIVOS)
                .ifPresent(reserva -> {
                    reserva.setEstadoReserva(EstadoReserva.CANCELADA);
                    reservaRepository.save(reserva);
                });
    }

    public void validarSinSolapamiento(Long habitacionId, LocalDate entrada, LocalDate salida) {
        if (reservaRepository.existeSolapamiento(habitacionId, entrada, salida, ESTADOS_ACTIVOS)) {
            throw new ReservaException(ReservaErrorCode.SOLAPAMIENTO);
        }
    }

    public boolean estaDisponibleParaReserva(Habitacion habitacion, LocalDate entrada, LocalDate salida) {
        inhabilitacionService.sincronizar(habitacion);

        if (habitacion.getEstado() != EstadoHabitacion.LIBRE) {
            return false;
        }
        if (inhabilitacionService.tieneLimpiezaPendiente(habitacion.getId())) {
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
                .build();

        return reservaRepository.save(reserva);
    }

    private void validarDisponibilidadOperativa(Habitacion habitacion, LocalDate entrada, LocalDate salida) {
        if (inhabilitacionService.tieneLimpiezaPendiente(habitacion.getId())) {
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
