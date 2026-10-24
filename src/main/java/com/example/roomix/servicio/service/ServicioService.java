package com.example.roomix.servicio.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.finanzas.domain.OrigenItem;
import com.example.roomix.finanzas.repository.FacturaItemRepository;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.personal.domain.Personal;
import com.example.roomix.personal.service.PersonalService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.domain.ServicioCatalogo;
import com.example.roomix.servicio.domain.SolicitudServicio;
import com.example.roomix.servicio.dto.ServicioDtos.AsignarRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CambiarEstadoRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CatalogoRequest;
import com.example.roomix.servicio.dto.ServicioDtos.CatalogoResponse;
import com.example.roomix.servicio.dto.ServicioDtos.SolicitudRequest;
import com.example.roomix.servicio.dto.ServicioDtos.SolicitudResponse;
import com.example.roomix.servicio.exception.ServicioErrorCode;
import com.example.roomix.servicio.exception.ServicioException;
import com.example.roomix.servicio.repository.ServicioCatalogoRepository;
import com.example.roomix.servicio.repository.SolicitudServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo de servicios adicionales y solicitudes de los huéspedes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioService {

    private static final Set<EstadoReserva> RESERVA_ACTIVA = Set.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    /** Transiciones permitidas del ciclo de una solicitud. */
    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES = Map.of(
            EstadoSolicitud.SOLICITADA, Set.of(EstadoSolicitud.EN_PROCESO, EstadoSolicitud.COMPLETADA, EstadoSolicitud.CANCELADA),
            EstadoSolicitud.EN_PROCESO, Set.of(EstadoSolicitud.COMPLETADA, EstadoSolicitud.CANCELADA),
            EstadoSolicitud.COMPLETADA, Set.of(),
            EstadoSolicitud.CANCELADA, Set.of()
    );

    private final ServicioCatalogoRepository catalogoRepository;
    private final SolicitudServicioRepository solicitudRepository;
    private final ReservaRepository reservaRepository;
    private final PersonalService personalService;
    private final FacturaItemRepository facturaItemRepository;

    // ----------------------------------------------------------------- Catálogo

    public List<CatalogoResponse> listarCatalogo(boolean soloActivos) {
        return (soloActivos
                ? catalogoRepository.findByActivoTrueOrderByCategoriaAscNombreAsc()
                : catalogoRepository.findAllByOrderByCategoriaAscNombreAsc())
                .stream().map(CatalogoResponse::from).toList();
    }

    @Transactional
    public CatalogoResponse crearServicio(CatalogoRequest request) {
        if (catalogoRepository.existsByNombreIgnoreCase(request.nombre().trim())) {
            throw new ServicioException(ServicioErrorCode.NOMBRE_DUPLICADO, request.nombre().trim());
        }
        ServicioCatalogo servicio = aplicar(ServicioCatalogo.builder().build(), request);
        return CatalogoResponse.from(catalogoRepository.save(servicio));
    }

    @Transactional
    public CatalogoResponse actualizarServicio(Long id, CatalogoRequest request) {
        ServicioCatalogo servicio = buscarServicio(id);
        if (catalogoRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre().trim(), id)) {
            throw new ServicioException(ServicioErrorCode.NOMBRE_DUPLICADO, request.nombre().trim());
        }
        return CatalogoResponse.from(catalogoRepository.save(aplicar(servicio, request)));
    }

    @Transactional
    public CatalogoResponse cambiarActivo(Long id, boolean activo) {
        ServicioCatalogo servicio = buscarServicio(id);
        servicio.setActivo(activo);
        return CatalogoResponse.from(catalogoRepository.save(servicio));
    }

    // --------------------------------------------------------------- Solicitudes

    public List<SolicitudResponse> listarSolicitudes(
            EstadoSolicitud estado,
            Long reservaId,
            Long huespedId,
            LocalDate fecha,
            Boolean activas
    ) {
        LocalDateTime desde = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime hasta = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;
        return solicitudRepository.findAll(
                        SolicitudServicioRepository.conFiltros(estado, reservaId, huespedId, desde, hasta, activas),
                        Sort.by(Sort.Direction.DESC, "fechaHoraSolicitud"))
                .stream().map(SolicitudResponse::from).toList();
    }

    @Transactional
    public SolicitudResponse crearSolicitud(SolicitudRequest request) {
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new ServicioException(ServicioErrorCode.RESERVA_NO_ENCONTRADA, request.reservaId()));
        if (!RESERVA_ACTIVA.contains(reserva.getEstadoReserva())) {
            throw new ServicioException(ServicioErrorCode.RESERVA_NO_ACTIVA);
        }
        ServicioCatalogo servicio = buscarServicio(request.servicioId());
        if (!servicio.isActivo()) {
            throw new ServicioException(ServicioErrorCode.SERVICIO_INACTIVO, servicio.getNombre());
        }

        SolicitudServicio solicitud = SolicitudServicio.builder()
                .servicio(servicio)
                .reserva(reserva)
                .habitacion(reserva.getHabitacion())
                .huesped(reserva.getHuesped())
                .cantidad(request.cantidad())
                .precioUnitario(servicio.getPrecio())
                .total(servicio.getPrecio().multiply(BigDecimal.valueOf(request.cantidad())))
                .fechaHoraProgramada(request.fechaHoraProgramada())
                .responsable(request.responsableId() != null ? personalService.buscarEntidad(request.responsableId()) : null)
                .notas(StringUtils.hasText(request.notas()) ? request.notas().trim() : null)
                .solicitadoPor(UsuarioActual.username())
                .fechaHoraSolicitud(LocalDateTime.now())
                .build();
        return SolicitudResponse.from(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudResponse asignar(Long id, AsignarRequest request) {
        SolicitudServicio solicitud = buscarSolicitud(id);
        validarNoCerrada(solicitud, solicitud.getEstado());
        Personal responsable = request.responsableId() != null ? personalService.buscarEntidad(request.responsableId()) : null;
        solicitud.setResponsable(responsable);
        solicitud.setFechaHoraProgramada(request.fechaHoraProgramada());
        return SolicitudResponse.from(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudResponse cambiarEstado(Long id, CambiarEstadoRequest request) {
        SolicitudServicio solicitud = buscarSolicitud(id);
        EstadoSolicitud nuevo = request.estado();
        if (!TRANSICIONES.get(solicitud.getEstado()).contains(nuevo)) {
            throw new ServicioException(ServicioErrorCode.TRANSICION_INVALIDA, solicitud.getEstado(), nuevo);
        }
        if (nuevo == EstadoSolicitud.CANCELADA) {
            if (!StringUtils.hasText(request.motivo())) {
                throw new ServicioException(ServicioErrorCode.MOTIVO_REQUERIDO);
            }
            if (facturaItemRepository.estaFacturado(OrigenItem.SERVICIO, solicitud.getId())) {
                throw new ServicioException(ServicioErrorCode.SOLICITUD_FACTURADA);
            }
            solicitud.setMotivoCancelacion(request.motivo().trim());
        }
        if (nuevo == EstadoSolicitud.COMPLETADA) {
            solicitud.setFechaHoraCompletado(LocalDateTime.now());
        }
        solicitud.setEstado(nuevo);
        return SolicitudResponse.from(solicitudRepository.save(solicitud));
    }

    /** Solicitudes cobrables (no canceladas) de una reserva, para la cuenta y la factura. */
    public List<SolicitudServicio> cobrablesDeReserva(Long reservaId) {
        return solicitudRepository.findByReservaIdAndEstadoNotOrderByFechaHoraSolicitudAsc(reservaId, EstadoSolicitud.CANCELADA);
    }

    private void validarNoCerrada(SolicitudServicio solicitud, EstadoSolicitud estado) {
        if (estado == EstadoSolicitud.COMPLETADA || estado == EstadoSolicitud.CANCELADA) {
            throw new ServicioException(ServicioErrorCode.TRANSICION_INVALIDA, estado, "asignación");
        }
    }

    private ServicioCatalogo aplicar(ServicioCatalogo servicio, CatalogoRequest request) {
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(StringUtils.hasText(request.descripcion()) ? request.descripcion().trim() : null);
        servicio.setCategoria(request.categoria());
        servicio.setPrecio(request.precio());
        servicio.setUnidad(request.unidad().trim());
        servicio.setDuracionMinutos(request.duracionMinutos());
        if (request.activo() != null) {
            servicio.setActivo(request.activo());
        }
        return servicio;
    }

    private ServicioCatalogo buscarServicio(Long id) {
        return catalogoRepository.findById(id)
                .orElseThrow(() -> new ServicioException(ServicioErrorCode.SERVICIO_NO_ENCONTRADO, id));
    }

    private SolicitudServicio buscarSolicitud(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ServicioException(ServicioErrorCode.SOLICITUD_NO_ENCONTRADA, id));
    }
}
