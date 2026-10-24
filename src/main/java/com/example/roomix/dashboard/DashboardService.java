package com.example.roomix.dashboard;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.dashboard.DashboardResponse.Alerta;
import com.example.roomix.dashboard.DashboardResponse.Finanzas;
import com.example.roomix.dashboard.DashboardResponse.Habitaciones;
import com.example.roomix.dashboard.DashboardResponse.Inventario;
import com.example.roomix.dashboard.DashboardResponse.MovimientoHuesped;
import com.example.roomix.dashboard.DashboardResponse.Operacion;
import com.example.roomix.evento.dto.EventoDtos.EventoResponse;
import com.example.roomix.evento.service.EventoService;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.finanzas.service.FinanzasService;
import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.MotivoInhabilitacion;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.habitacion.service.HabitacionInhabilitacionService;
import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.domain.Incidencia;
import com.example.roomix.incidencia.repository.IncidenciaRepository;
import com.example.roomix.inventario.dto.ArticuloInventarioResponse;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.repository.SolicitudServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Arma el panel del día según el rol del usuario autenticado.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {

    private static final List<EstadoIncidencia> CERRADAS = List.of(EstadoIncidencia.FINALIZADA, EstadoIncidencia.CANCELADA);
    private static final List<EstadoIncidencia> OCUPAN_PERSONAL = List.of(EstadoIncidencia.ASIGNADA, EstadoIncidencia.EN_PROGRESO);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final HabitacionRepository habitacionRepository;
    private final HabitacionInhabilitacionService inhabilitacionService;
    private final ReservaRepository reservaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final SolicitudServicioRepository solicitudRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final FinanzasService finanzasService;
    private final FacturaRepository facturaRepository;
    private final EventoService eventoService;

    public DashboardResponse obtener() {
        String rol = UsuarioActual.rol();
        LocalDate hoy = LocalDate.now();
        List<Alerta> alertas = new ArrayList<>();

        boolean veHabitaciones = es(rol, Rol.ADMIN, Rol.RECEPCION, Rol.HOUSEKEEPING);
        boolean veRecepcion = es(rol, Rol.ADMIN, Rol.RECEPCION);
        boolean veOperacion = es(rol, Rol.ADMIN, Rol.RECEPCION, Rol.HOUSEKEEPING);
        boolean veInventario = es(rol, Rol.ADMIN, Rol.ALMACEN, Rol.HOUSEKEEPING);
        boolean veFinanzas = es(rol, Rol.ADMIN, Rol.FINANZAS);
        boolean veEventos = es(rol, Rol.ADMIN, Rol.EVENTOS, Rol.RECEPCION);

        Habitaciones habitaciones = veHabitaciones ? habitaciones(alertas) : null;

        List<MovimientoHuesped> llegadas = null;
        List<MovimientoHuesped> salidas = null;
        if (veRecepcion) {
            llegadas = reservaRepository.findAll((root, q, cb) -> cb.and(
                            cb.equal(root.get("estadoReserva"), EstadoReserva.CONFIRMADA),
                            cb.lessThanOrEqualTo(root.get("fechaEntrada"), hoy)))
                    .stream()
                    .sorted(Comparator.comparing(Reserva::getFechaEntrada).thenComparing(r -> r.getHabitacion().getNumero()))
                    .map(r -> movimiento(r, r.getFechaEntrada().isBefore(hoy)))
                    .toList();
            salidas = reservaRepository.findAll((root, q, cb) -> cb.and(
                            cb.equal(root.get("estadoReserva"), EstadoReserva.EN_CURSO),
                            cb.lessThanOrEqualTo(root.get("fechaSalida"), hoy)))
                    .stream()
                    .sorted(Comparator.comparing(Reserva::getFechaSalida).thenComparing(r -> r.getHabitacion().getNumero()))
                    .map(r -> movimiento(r, r.getFechaSalida().isBefore(hoy)))
                    .toList();
            long llegadasAtrasadas = llegadas.stream().filter(MovimientoHuesped::atrasada).count();
            long salidasAtrasadas = salidas.stream().filter(MovimientoHuesped::atrasada).count();
            if (llegadasAtrasadas > 0) {
                alertas.add(new Alerta("ADVERTENCIA", "RESERVAS",
                        llegadasAtrasadas + " reserva(s) con llegada vencida sin check-in (posible no-show)", "/reservas"));
            }
            if (salidasAtrasadas > 0) {
                alertas.add(new Alerta("CRITICA", "RESERVAS",
                        salidasAtrasadas + " estadía(s) con salida vencida sin check-out", "/inicio"));
            }
        }

        Operacion operacion = null;
        if (veOperacion) {
            List<Incidencia> activas = incidenciaRepository.buscarConFiltros(null, null, null, true, CERRADAS);
            long sinAsignar = activas.stream().filter(i -> i.getPersonalAsignado() == null).count();
            long servicios = solicitudRepository.countByEstadoIn(List.of(EstadoSolicitud.SOLICITADA, EstadoSolicitud.EN_PROCESO));
            long personal = incidenciaRepository.findPersonalIdsOcupados(OCUPAN_PERSONAL).size();
            operacion = new Operacion(sinAsignar, activas.size(), servicios, personal);
            if (sinAsignar > 0) {
                alertas.add(new Alerta("ADVERTENCIA", "INCIDENCIAS",
                        sinAsignar + " servicio(s) de limpieza o mantenimiento sin personal asignado", "/incidencias"));
            }
        }

        Inventario inventario = null;
        if (veInventario) {
            List<ArticuloInventarioResponse> criticos = articuloRepository.findConStockBajo().stream()
                    .map(ArticuloInventarioResponse::from)
                    .sorted(Comparator.comparing(ArticuloInventarioResponse::nombre))
                    .toList();
            inventario = new Inventario(criticos.size(), criticos.stream().limit(5).toList());
            if (!criticos.isEmpty()) {
                alertas.add(new Alerta("ADVERTENCIA", "INVENTARIO",
                        criticos.size() + " artículo(s) en o por debajo del stock mínimo", "/inventario"));
            }
        }

        Finanzas finanzas = null;
        if (veFinanzas) {
            LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
            Map<CategoriaFinanciera, BigDecimal> mes = finanzasService.totalesPorCategoria(hoy.withDayOfMonth(1), hoy);
            BigDecimal egresosMes = mes.entrySet().stream()
                    .filter(e -> e.getKey().getTipo() == TipoMovimiento.EGRESO)
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ingresosMes = mes.entrySet().stream()
                    .filter(e -> e.getKey().getTipo() == TipoMovimiento.INGRESO)
                    .map(Map.Entry::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            finanzas = new Finanzas(
                    finanzasService.ingresosEntre(hoy, hoy),
                    finanzasService.ingresosEntre(lunes, hoy),
                    ingresosMes,
                    egresosMes,
                    facturaRepository.saldoPendiente(List.of(EstadoFactura.PENDIENTE, EstadoFactura.PAGADA_PARCIAL))
            );
        }

        List<EventoResponse> eventos = null;
        if (veEventos) {
            eventos = eventoService.proximos(7);
            eventos.stream()
                    .filter(e -> e.fechaHoraInicio().toLocalDate().equals(hoy))
                    .forEach(e -> alertas.add(new Alerta("INFO", "EVENTOS",
                            "Hoy: " + e.nombre() + " en " + e.salonNombre() + " (" + e.fechaHoraInicio().format(HORA) + ")",
                            "/eventos/" + e.id())));
        }

        alertas.sort(Comparator.comparing(a -> switch (a.nivel()) {
            case "CRITICA" -> 0;
            case "ADVERTENCIA" -> 1;
            default -> 2;
        }));
        return new DashboardResponse(hoy, rol, habitaciones, llegadas, salidas, operacion, inventario, finanzas,
                eventos, alertas);
    }

    private Habitaciones habitaciones(List<Alerta> alertas) {
        List<Habitacion> todas = habitacionRepository.findAll();
        todas.forEach(inhabilitacionService::sincronizar);
        habitacionRepository.saveAll(todas);

        int libres = contar(todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.LIBRE));
        int reservadas = contar(todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.RESERVADO));
        int ocupadas = contar(todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.OCUPADO));
        int limpieza = contar(todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.INHABILITADO
                && h.getMotivoInhabilitacion() == MotivoInhabilitacion.POST_CHECKOUT));
        int mantenimiento = contar(todas.stream().filter(h -> h.getEstado() == EstadoHabitacion.INHABILITADO
                && h.getMotivoInhabilitacion() != MotivoInhabilitacion.POST_CHECKOUT));
        double ocupacion = todas.isEmpty() ? 0
                : BigDecimal.valueOf(ocupadas * 100.0 / todas.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
        if (mantenimiento > 0) {
            alertas.add(new Alerta("INFO", "HABITACIONES",
                    mantenimiento + " habitación(es) en mantenimiento hoy", "/habitaciones"));
        }
        return new Habitaciones(todas.size(), libres, reservadas, ocupadas, limpieza, mantenimiento, ocupacion);
    }

    private static MovimientoHuesped movimiento(Reserva r, boolean atrasada) {
        return new MovimientoHuesped(r.getId(), r.getHabitacion().getId(), r.getHabitacion().getNumero(),
                r.getHuesped().nombreCompleto(), r.getFechaEntrada(), r.getFechaSalida(), r.getCantidadNoches(), atrasada);
    }

    private static int contar(Stream<?> stream) {
        return (int) stream.count();
    }

    private static boolean es(String rol, Rol... roles) {
        if (rol == null) {
            return false;
        }
        for (Rol r : roles) {
            if (r.name().equals(rol)) {
                return true;
            }
        }
        return false;
    }
}
