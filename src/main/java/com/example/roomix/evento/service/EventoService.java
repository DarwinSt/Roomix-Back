package com.example.roomix.evento.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.evento.domain.Evento;
import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoAcceso;
import com.example.roomix.evento.domain.EventoParticipante;
import com.example.roomix.evento.domain.EventoRecurso;
import com.example.roomix.evento.domain.Salon;
import com.example.roomix.evento.dto.EventoDtos.CambiarEstadoRequest;
import com.example.roomix.evento.dto.EventoDtos.EventoRequest;
import com.example.roomix.evento.dto.EventoDtos.EventoResponse;
import com.example.roomix.evento.dto.EventoDtos.ParticipanteRequest;
import com.example.roomix.evento.dto.EventoDtos.RecursoRequest;
import com.example.roomix.evento.dto.EventoDtos.ReporteEventos;
import com.example.roomix.evento.dto.EventoDtos.ReporteSalon;
import com.example.roomix.evento.dto.EventoDtos.SalonRequest;
import com.example.roomix.evento.dto.EventoDtos.SalonResponse;
import com.example.roomix.evento.exception.EventoErrorCode;
import com.example.roomix.evento.exception.EventoException;
import com.example.roomix.evento.repository.EventoRepository;
import com.example.roomix.evento.repository.SalonRepository;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.service.HuespedService;
import com.example.roomix.personal.service.PersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Planificación y seguimiento de eventos en las instalaciones del hotel.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoService {

    private static final Set<EstadoEvento> ESTADOS_VIGENTES = Set.of(EstadoEvento.PROGRAMADO, EstadoEvento.EN_CURSO);

    private static final Map<EstadoEvento, Set<EstadoEvento>> TRANSICIONES = Map.of(
            EstadoEvento.PROGRAMADO, Set.of(EstadoEvento.EN_CURSO, EstadoEvento.CANCELADO),
            EstadoEvento.EN_CURSO, Set.of(EstadoEvento.FINALIZADO, EstadoEvento.CANCELADO),
            EstadoEvento.FINALIZADO, Set.of(),
            EstadoEvento.CANCELADO, Set.of()
    );

    private final EventoRepository eventoRepository;
    private final SalonRepository salonRepository;
    private final PersonalService personalService;
    private final HuespedService huespedService;
    private final FacturaRepository facturaRepository;

    // ---------------------------------------------------------------- Salones

    public List<SalonResponse> listarSalones() {
        return salonRepository.findAllByOrderByNombreAsc().stream().map(SalonResponse::from).toList();
    }

    @Transactional
    public SalonResponse guardarSalon(Long id, SalonRequest request) {
        String nombre = request.nombre().trim();
        boolean duplicado = id == null
                ? salonRepository.existsByNombreIgnoreCase(nombre)
                : salonRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id);
        if (duplicado) {
            throw new EventoException(EventoErrorCode.NOMBRE_SALON_DUPLICADO, nombre);
        }
        Salon salon = id == null ? Salon.builder().build() : buscarSalon(id);
        salon.setNombre(nombre);
        salon.setCapacidad(request.capacidad());
        salon.setDescripcion(StringUtils.hasText(request.descripcion()) ? request.descripcion().trim() : null);
        salon.setTarifaHora(request.tarifaHora());
        if (request.activo() != null) {
            salon.setActivo(request.activo());
        }
        return SalonResponse.from(salonRepository.save(salon));
    }

    // ---------------------------------------------------------------- Eventos

    public List<EventoResponse> listar(LocalDate desde, LocalDate hasta, EstadoEvento estado) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().minusDays(30);
        LocalDate fin = hasta != null ? hasta : LocalDate.now().plusDays(90);
        return eventoRepository.enRango(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .filter(e -> estado == null || e.getEstado() == estado)
                .map(this::responder)
                .toList();
    }

    public EventoResponse obtener(Long id) {
        return responder(buscar(id));
    }

    /** Eventos programados o en curso que inician en los próximos días (alertas del dashboard). */
    public List<EventoResponse> proximos(int dias) {
        LocalDateTime ahora = LocalDateTime.now();
        return eventoRepository.findByEstadoInAndFechaHoraInicioBetweenOrderByFechaHoraInicioAsc(
                        ESTADOS_VIGENTES, ahora.minusHours(12), ahora.plusDays(dias))
                .stream().map(this::responder).toList();
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Evento evento = Evento.builder().creadoPor(UsuarioActual.username()).build();
        aplicar(evento, request);
        return responder(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse actualizar(Long id, EventoRequest request) {
        Evento evento = buscar(id);
        validarEditable(evento);
        aplicar(evento, request);
        return responder(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse cambiarEstado(Long id, CambiarEstadoRequest request) {
        Evento evento = buscar(id);
        EstadoEvento nuevo = request.estado();
        if (!TRANSICIONES.get(evento.getEstado()).contains(nuevo)) {
            throw new EventoException(EventoErrorCode.TRANSICION_INVALIDA, evento.getEstado(), nuevo);
        }
        if (nuevo == EstadoEvento.CANCELADO) {
            if (!StringUtils.hasText(request.motivo())) {
                throw new EventoException(EventoErrorCode.MOTIVO_REQUERIDO);
            }
            evento.setMotivoCancelacion(request.motivo().trim());
        }
        evento.setEstado(nuevo);
        return responder(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse agregarParticipante(Long eventoId, ParticipanteRequest request) {
        Evento evento = buscar(eventoId);
        validarEditable(evento);
        if (evento.getParticipantes().size() >= evento.getCapacidad()) {
            throw new EventoException(EventoErrorCode.CUPO_LLENO, evento.getCapacidad());
        }

        EventoParticipante participante = EventoParticipante.builder()
                .evento(evento)
                .confirmado(Boolean.TRUE.equals(request.confirmado()))
                .fechaHoraRegistro(LocalDateTime.now())
                .build();

        if (request.huespedId() != null) {
            Huesped huesped = huespedService.buscarActivo(request.huespedId());
            boolean yaInscrito = evento.getParticipantes().stream()
                    .anyMatch(p -> p.getHuesped() != null && p.getHuesped().getId().equals(huesped.getId()));
            if (yaInscrito) {
                throw new EventoException(EventoErrorCode.PARTICIPANTE_DUPLICADO, huesped.nombreCompleto());
            }
            participante.setHuesped(huesped);
            participante.setNombre(huesped.nombreCompleto());
            participante.setDocumento(huesped.getNumeroDocumento());
            participante.setEmail(huesped.getEmail());
            participante.setTelefono(huesped.getTelefono());
        } else {
            if (evento.getTipoAcceso() == TipoAcceso.SOLO_HUESPEDES) {
                throw new EventoException(EventoErrorCode.SOLO_HUESPEDES);
            }
            if (!StringUtils.hasText(request.nombre())) {
                throw new EventoException(EventoErrorCode.NOMBRE_PARTICIPANTE_REQUERIDO);
            }
            String documento = StringUtils.hasText(request.documento()) ? request.documento().trim() : null;
            if (documento != null && evento.getParticipantes().stream().anyMatch(p -> documento.equalsIgnoreCase(p.getDocumento()))) {
                throw new EventoException(EventoErrorCode.PARTICIPANTE_DUPLICADO, documento);
            }
            participante.setNombre(request.nombre().trim());
            participante.setDocumento(documento);
            participante.setEmail(StringUtils.hasText(request.email()) ? request.email().trim() : null);
            participante.setTelefono(StringUtils.hasText(request.telefono()) ? request.telefono().trim() : null);
        }
        evento.getParticipantes().add(participante);
        return responder(eventoRepository.save(evento));
    }

    /** Marca confirmación (antes del evento) y asistencia (durante o al final). */
    @Transactional
    public EventoResponse actualizarParticipante(Long eventoId, Long participanteId, Boolean confirmado, Boolean asistio) {
        Evento evento = buscar(eventoId);
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new EventoException(EventoErrorCode.EVENTO_CERRADO, "cancelado");
        }
        EventoParticipante participante = evento.getParticipantes().stream()
                .filter(p -> p.getId().equals(participanteId))
                .findFirst()
                .orElseThrow(() -> new EventoException(EventoErrorCode.PARTICIPANTE_NO_ENCONTRADO, participanteId));
        if (confirmado != null) {
            participante.setConfirmado(confirmado);
        }
        if (asistio != null) {
            participante.setAsistio(asistio);
            if (asistio) {
                participante.setConfirmado(true);
            }
        }
        return responder(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse eliminarParticipante(Long eventoId, Long participanteId) {
        Evento evento = buscar(eventoId);
        validarEditable(evento);
        boolean eliminado = evento.getParticipantes().removeIf(p -> p.getId().equals(participanteId));
        if (!eliminado) {
            throw new EventoException(EventoErrorCode.PARTICIPANTE_NO_ENCONTRADO, participanteId);
        }
        return responder(eventoRepository.save(evento));
    }

    public ReporteEventos reporte(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        List<Evento> eventos = eventoRepository.enRango(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        Map<EstadoEvento, Long> porEstado = new EnumMap<>(EstadoEvento.class);
        for (EstadoEvento e : EstadoEvento.values()) {
            porEstado.put(e, 0L);
        }
        Map<String, long[]> conteoSalon = new LinkedHashMap<>();
        Map<String, BigDecimal> valorSalon = new LinkedHashMap<>();
        long inscritos = 0;
        long asistentes = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal facturado = BigDecimal.ZERO;
        BigDecimal recaudado = BigDecimal.ZERO;

        for (Evento e : eventos) {
            porEstado.merge(e.getEstado(), 1L, Long::sum);
            if (e.getEstado() == EstadoEvento.CANCELADO) {
                continue;
            }
            inscritos += e.getParticipantes().size();
            asistentes += e.getParticipantes().stream().filter(EventoParticipante::isAsistio).count();
            valorTotal = valorTotal.add(e.valorTotal());
            String salon = e.getSalon().getNombre();
            long[] c = conteoSalon.computeIfAbsent(salon, k -> new long[2]);
            c[0]++;
            c[1] += e.getParticipantes().size();
            valorSalon.merge(salon, e.valorTotal(), BigDecimal::add);
            Factura factura = facturaRepository.findFirstByEventoIdAndEstadoNot(e.getId(), EstadoFactura.ANULADA).orElse(null);
            if (factura != null) {
                facturado = facturado.add(factura.getTotal());
                recaudado = recaudado.add(factura.getTotalPagado());
            }
        }

        List<ReporteSalon> porSalon = new ArrayList<>();
        conteoSalon.forEach((salon, c) -> porSalon.add(new ReporteSalon(salon, c[0], c[1], valorSalon.get(salon))));
        return new ReporteEventos(inicio, fin, eventos.size(), porEstado, inscritos, asistentes,
                valorTotal, facturado, recaudado, porSalon);
    }

    public Evento buscar(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new EventoException(EventoErrorCode.EVENTO_NO_ENCONTRADO, id));
    }

    private void aplicar(Evento evento, EventoRequest request) {
        if (!request.fechaHoraFin().isAfter(request.fechaHoraInicio())) {
            throw new EventoException(EventoErrorCode.FECHAS_INVALIDAS);
        }
        Salon salon = buscarSalon(request.salonId());
        if (!salon.isActivo()) {
            throw new EventoException(EventoErrorCode.SALON_INACTIVO, salon.getNombre());
        }
        if (request.capacidad() > salon.getCapacidad()) {
            throw new EventoException(EventoErrorCode.CAPACIDAD_SALON, request.capacidad(), salon.getNombre(), salon.getCapacidad());
        }
        if (request.capacidad() < evento.getParticipantes().size()) {
            throw new EventoException(EventoErrorCode.CUPO_LLENO, evento.getParticipantes().size());
        }
        Long excluir = evento.getId() != null ? evento.getId() : -1L;
        if (eventoRepository.existeCruce(salon.getId(), request.fechaHoraInicio(), request.fechaHoraFin(), excluir, ESTADOS_VIGENTES)) {
            throw new EventoException(EventoErrorCode.SALON_OCUPADO, salon.getNombre());
        }

        evento.setNombre(request.nombre().trim());
        evento.setDescripcion(texto(request.descripcion()));
        evento.setTipo(request.tipo());
        evento.setTipoAcceso(request.tipoAcceso());
        evento.setFechaHoraInicio(request.fechaHoraInicio());
        evento.setFechaHoraFin(request.fechaHoraFin());
        evento.setSalon(salon);
        evento.setResponsable(request.responsableId() != null ? personalService.buscarEntidad(request.responsableId()) : null);
        evento.setCapacidad(request.capacidad());
        evento.setOrganizadorNombre(request.organizadorNombre().trim());
        evento.setOrganizadorDocumento(texto(request.organizadorDocumento()));
        evento.setOrganizadorTelefono(texto(request.organizadorTelefono()));
        evento.setOrganizadorEmail(texto(request.organizadorEmail()));
        evento.setValorBase(request.valorBase());
        evento.setNotas(texto(request.notas()));

        evento.getRecursos().clear();
        if (request.recursos() != null) {
            for (RecursoRequest r : request.recursos()) {
                evento.getRecursos().add(EventoRecurso.builder()
                        .evento(evento)
                        .tipo(r.tipo())
                        .descripcion(r.descripcion().trim())
                        .cantidad(r.cantidad())
                        .valorUnitario(r.valorUnitario())
                        .total(r.valorUnitario().multiply(BigDecimal.valueOf(r.cantidad())))
                        .build());
            }
        }
    }

    private void validarEditable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.FINALIZADO || evento.getEstado() == EstadoEvento.CANCELADO) {
            throw new EventoException(EventoErrorCode.EVENTO_CERRADO, evento.getEstado().name().toLowerCase());
        }
    }

    private EventoResponse responder(Evento evento) {
        Factura factura = evento.getId() == null ? null
                : facturaRepository.findFirstByEventoIdAndEstadoNot(evento.getId(), EstadoFactura.ANULADA).orElse(null);
        return EventoResponse.from(evento, factura);
    }

    private Salon buscarSalon(Long id) {
        return salonRepository.findById(id)
                .orElseThrow(() -> new EventoException(EventoErrorCode.SALON_NO_ENCONTRADO, id));
    }

    private static String texto(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
