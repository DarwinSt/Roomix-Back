package com.example.roomix.incidencia.service;

import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.habitacion.service.HabitacionInhabilitacionService;
import com.example.roomix.incidencia.domain.AlcanceIncidencia;
import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.domain.Incidencia;
import com.example.roomix.incidencia.domain.IncidenciaTarea;
import com.example.roomix.incidencia.domain.TipoIncidencia;
import com.example.roomix.incidencia.dto.ActualizarTareaIncidenciaRequest;
import com.example.roomix.incidencia.dto.AsignarIncidenciaRequest;
import com.example.roomix.incidencia.dto.CrearIncidenciaRequest;
import com.example.roomix.incidencia.dto.IncidenciaResponse;
import com.example.roomix.incidencia.exception.IncidenciaErrorCode;
import com.example.roomix.incidencia.exception.IncidenciaException;
import com.example.roomix.incidencia.repository.IncidenciaRepository;
import com.example.roomix.personal.domain.Personal;
import com.example.roomix.personal.service.PersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidenciaService {

    private static final List<EstadoIncidencia> ESTADOS_CERRADOS = List.of(
            EstadoIncidencia.FINALIZADA,
            EstadoIncidencia.CANCELADA
    );

    private static final List<EstadoIncidencia> ESTADOS_PERSONAL_OCUPADO = List.of(
            EstadoIncidencia.ASIGNADA,
            EstadoIncidencia.EN_PROGRESO
    );

    private static final Map<TipoIncidencia, List<String>> TAREAS_POR_TIPO = Map.of(
            TipoIncidencia.LIMPIEZA, List.of(
                    "Preparar materiales de limpieza",
                    "Ingresar a la habitación",
                    "Limpiar baño",
                    "Cambiar ropa de cama",
                    "Limpiar superficies y piso",
                    "Revisión final del servicio"
            ),
            TipoIncidencia.MANTENIMIENTO, List.of(
                    "Revisión técnica inicial",
                    "Ejecutar reparación o adecuación",
                    "Verificación de funcionamiento",
                    "Cierre de mantenimiento"
            ),
            TipoIncidencia.SERVICIO_CUARTO, List.of(
                    "Preparar pedido en cocina",
                    "Entregar servicio en habitación",
                    "Retirar vajilla y residuos",
                    "Confirmar servicio con huésped o recepción"
            ),
            TipoIncidencia.OTRO, List.of(
                    "Iniciar servicio",
                    "Ejecutar tareas del servicio",
                    "Verificación final"
            )
    );

    private static final Map<TipoIncidencia, List<String>> TAREAS_ZONA_COMUN = Map.of(
            TipoIncidencia.MANTENIMIENTO, List.of(
                    "Revisión inicial del área",
                    "Ejecutar reparación o adecuación",
                    "Verificación de funcionamiento",
                    "Cierre de mantenimiento"
            ),
            TipoIncidencia.OTRO, List.of(
                    "Iniciar servicio en zona común",
                    "Ejecutar tareas del servicio",
                    "Verificación final"
            )
    );

    private final IncidenciaRepository incidenciaRepository;
    private final HabitacionRepository habitacionRepository;
    private final HabitacionInhabilitacionService inhabilitacionService;
    private final PersonalService personalService;

    public List<IncidenciaResponse> listar(
            EstadoIncidencia estado,
            Long habitacionId,
            Long personalId,
            Boolean activas
    ) {
        return incidenciaRepository.buscarConFiltros(
                        estado,
                        habitacionId,
                        personalId,
                        activas,
                        ESTADOS_CERRADOS
                )
                .stream()
                .map(IncidenciaResponse::from)
                .toList();
    }

    public IncidenciaResponse obtenerPorId(Long id) {
        return IncidenciaResponse.from(buscarEntidad(id));
    }

    public List<IncidenciaResponse> listarActivasPorHabitacion(Long habitacionId) {
        return incidenciaRepository.findActivasPorHabitacion(habitacionId, ESTADOS_CERRADOS)
                .stream()
                .map(IncidenciaResponse::from)
                .toList();
    }

    @Transactional
    public IncidenciaResponse crear(CrearIncidenciaRequest request) {
        AlcanceIncidencia alcance = request.alcance();
        Habitacion habitacion = null;
        String ubicacion = normalizarUbicacion(request.ubicacion());
        String referenciaTitulo;

        if (alcance == AlcanceIncidencia.HABITACION) {
            if (request.habitacionId() == null) {
                throw new IncidenciaException(IncidenciaErrorCode.HABITACION_REQUERIDA);
            }
            habitacion = habitacionRepository.findById(request.habitacionId())
                    .orElseThrow(() -> new IncidenciaException(
                            IncidenciaErrorCode.HABITACION_NO_ENCONTRADA,
                            request.habitacionId()
                    ));
            inhabilitacionService.sincronizar(habitacion);
            habitacionRepository.save(habitacion);
            validarCreacionServicio(habitacion, request.tipo());
            referenciaTitulo = "habitación " + habitacion.getNumero();

            if (request.tipo() == TipoIncidencia.MANTENIMIENTO && request.fechaHoraProgramada() == null) {
                throw new IncidenciaException(IncidenciaErrorCode.FECHA_PROGRAMADA_REQUERIDA);
            }

            if (incidenciaRepository.existsByHabitacionIdAndTipoAndEstadoNotIn(
                    habitacion.getId(),
                    request.tipo(),
                    ESTADOS_CERRADOS
            )) {
                throw new IncidenciaException(
                        IncidenciaErrorCode.INCIDENCIA_ACTIVA_EXISTENTE,
                        request.tipo(),
                        habitacion.getNumero()
                );
            }
        } else {
            if (ubicacion == null || ubicacion.isBlank()) {
                throw new IncidenciaException(IncidenciaErrorCode.UBICACION_REQUERIDA);
            }
            validarTipoZonaComun(request.tipo());
            referenciaTitulo = ubicacion;
        }

        Incidencia incidencia = Incidencia.builder()
                .alcance(alcance)
                .habitacion(habitacion)
                .ubicacion(alcance == AlcanceIncidencia.ZONA_COMUN ? ubicacion : null)
                .tipo(request.tipo())
                .titulo(tituloParaTipo(request.tipo(), alcance, referenciaTitulo))
                .descripcion(request.descripcion() != null && !request.descripcion().isBlank()
                        ? request.descripcion().trim()
                        : descripcionPorDefecto(request.tipo(), alcance))
                .estado(EstadoIncidencia.CREADA)
                .fechaHoraProgramada(request.fechaHoraProgramada())
                .build();

        agregarTareas(incidencia, request.tipo(), alcance);
        Incidencia guardada = incidenciaRepository.save(incidencia);
        sincronizarHabitacionSiAplica(guardada);
        return IncidenciaResponse.from(guardada);
    }

    @Transactional
    public IncidenciaResponse crearLimpiezaPostCheckout(Habitacion habitacion) {
        if (incidenciaRepository.existsByHabitacionIdAndTipoAndEstadoNotIn(
                habitacion.getId(),
                TipoIncidencia.LIMPIEZA,
                ESTADOS_CERRADOS
        )) {
            return incidenciaRepository.findActivasPorHabitacion(habitacion.getId(), ESTADOS_CERRADOS)
                    .stream()
                    .filter(i -> i.getTipo() == TipoIncidencia.LIMPIEZA)
                    .findFirst()
                    .map(IncidenciaResponse::from)
                    .orElseThrow();
        }

        Incidencia incidencia = Incidencia.builder()
                .alcance(AlcanceIncidencia.HABITACION)
                .habitacion(habitacion)
                .tipo(TipoIncidencia.LIMPIEZA)
                .titulo(tituloParaTipo(TipoIncidencia.LIMPIEZA, AlcanceIncidencia.HABITACION, "habitación " + habitacion.getNumero()))
                .descripcion("Limpieza automática tras check-out del huésped.")
                .estado(EstadoIncidencia.CREADA)
                .build();
        agregarTareas(incidencia, TipoIncidencia.LIMPIEZA, AlcanceIncidencia.HABITACION);
        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public IncidenciaResponse asignar(Long id, AsignarIncidenciaRequest request) {
        Incidencia incidencia = buscarEntidad(id);
        validarEstado(incidencia, EstadoIncidencia.CREADA);

        Personal personal = personalService.buscarEntidad(request.personalId());
        if (!personal.isActivo()) {
            throw new IncidenciaException(IncidenciaErrorCode.PERSONAL_INACTIVO, personal.getNombre());
        }

        if (incidenciaRepository.personalTieneIncidenciaActiva(
                personal.getId(),
                ESTADOS_PERSONAL_OCUPADO,
                incidencia.getId()
        )) {
            throw new IncidenciaException(IncidenciaErrorCode.PERSONAL_OCUPADO, personal.getNombre());
        }

        incidencia.setPersonalAsignado(personal);
        incidencia.setEstado(EstadoIncidencia.ASIGNADA);
        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public IncidenciaResponse iniciar(Long id) {
        Incidencia incidencia = buscarEntidad(id);
        validarEstado(incidencia, EstadoIncidencia.ASIGNADA);
        if (incidencia.getPersonalAsignado() == null) {
            throw new IncidenciaException(IncidenciaErrorCode.SIN_PERSONAL_ASIGNADO);
        }

        incidencia.setEstado(EstadoIncidencia.EN_PROGRESO);
        marcarPrimeraTareaDeIngreso(incidencia);
        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public IncidenciaResponse actualizarTarea(Long incidenciaId, Long tareaId, ActualizarTareaIncidenciaRequest request) {
        Incidencia incidencia = buscarEntidad(incidenciaId);
        if (incidencia.getEstado() != EstadoIncidencia.EN_PROGRESO) {
            throw new IncidenciaException(IncidenciaErrorCode.ESTADO_INVALIDO, incidencia.getEstado());
        }

        IncidenciaTarea tarea = incidencia.getTareas().stream()
                .filter(t -> t.getId().equals(tareaId))
                .findFirst()
                .orElseThrow(() -> new IncidenciaException(IncidenciaErrorCode.TAREA_NO_ENCONTRADA, tareaId));

        boolean completada = Boolean.TRUE.equals(request.completada());
        tarea.setCompletada(completada);
        tarea.setFechaHoraCompletado(completada ? LocalDateTime.now() : null);

        return IncidenciaResponse.from(incidenciaRepository.save(incidencia));
    }

    @Transactional
    public IncidenciaResponse finalizar(Long id) {
        Incidencia incidencia = buscarEntidad(id);
        if (incidencia.getEstado() != EstadoIncidencia.EN_PROGRESO) {
            throw new IncidenciaException(IncidenciaErrorCode.ESTADO_INVALIDO, incidencia.getEstado());
        }

        boolean todasCompletas = incidencia.getTareas().stream().allMatch(IncidenciaTarea::isCompletada);
        if (!todasCompletas) {
            throw new IncidenciaException(IncidenciaErrorCode.TAREAS_INCOMPLETAS);
        }

        incidencia.setEstado(EstadoIncidencia.FINALIZADA);
        incidencia.setFechaHoraFinalizacion(LocalDateTime.now());
        incidenciaRepository.save(incidencia);
        sincronizarHabitacionSiAplica(incidencia);
        return IncidenciaResponse.from(incidencia);
    }

    @Transactional
    public IncidenciaResponse cancelar(Long id) {
        Incidencia incidencia = buscarEntidad(id);
        if (ESTADOS_CERRADOS.contains(incidencia.getEstado())) {
            throw new IncidenciaException(IncidenciaErrorCode.ESTADO_INVALIDO, incidencia.getEstado());
        }
        incidencia.setEstado(EstadoIncidencia.CANCELADA);
        incidencia.setFechaHoraFinalizacion(LocalDateTime.now());
        incidenciaRepository.save(incidencia);

        sincronizarHabitacionSiAplica(incidencia);
        return IncidenciaResponse.from(incidencia);
    }

    private void sincronizarHabitacionSiAplica(Incidencia incidencia) {
        if (incidencia.getHabitacion() != null) {
            inhabilitacionService.sincronizar(incidencia.getHabitacion());
            habitacionRepository.save(incidencia.getHabitacion());
        }
    }

    private void validarTipoZonaComun(TipoIncidencia tipo) {
        if (tipo != TipoIncidencia.MANTENIMIENTO && tipo != TipoIncidencia.OTRO) {
            throw new IncidenciaException(
                    IncidenciaErrorCode.ALCANCE_TIPO_INVALIDO,
                    tipo,
                    AlcanceIncidencia.ZONA_COMUN
            );
        }
    }

    private String normalizarUbicacion(String ubicacion) {
        return ubicacion != null ? ubicacion.trim() : null;
    }

    private void validarCreacionServicio(Habitacion habitacion, TipoIncidencia tipo) {
        if (habitacion.getEstado() == EstadoHabitacion.INHABILITADO) {
            return;
        }
        if (habitacion.getEstado() == EstadoHabitacion.OCUPADO) {
            if (tipo == TipoIncidencia.LIMPIEZA || tipo == TipoIncidencia.MANTENIMIENTO) {
                throw new IncidenciaException(
                        IncidenciaErrorCode.TIPO_NO_PERMITIDO,
                        tipo,
                        habitacion.getEstado()
                );
            }
            return;
        }
        if (habitacion.getEstado() == EstadoHabitacion.LIBRE
                || habitacion.getEstado() == EstadoHabitacion.RESERVADO) {
            if (tipo == TipoIncidencia.SERVICIO_CUARTO || tipo == TipoIncidencia.LIMPIEZA) {
                throw new IncidenciaException(
                        IncidenciaErrorCode.TIPO_NO_PERMITIDO,
                        tipo,
                        habitacion.getEstado()
                );
            }
            return;
        }
        throw new IncidenciaException(
                IncidenciaErrorCode.HABITACION_ESTADO_INVALIDO,
                tipo,
                habitacion.getNumero()
        );
    }

    private Incidencia buscarEntidad(Long id) {
        return incidenciaRepository.findDetalleById(id)
                .orElseThrow(() -> new IncidenciaException(IncidenciaErrorCode.INCIDENCIA_NO_ENCONTRADA, id));
    }

    private void validarEstado(Incidencia incidencia, EstadoIncidencia esperado) {
        if (incidencia.getEstado() != esperado) {
            throw new IncidenciaException(IncidenciaErrorCode.ESTADO_INVALIDO, incidencia.getEstado());
        }
    }

    private void agregarTareas(Incidencia incidencia, TipoIncidencia tipo, AlcanceIncidencia alcance) {
        Map<TipoIncidencia, List<String>> mapa = alcance == AlcanceIncidencia.ZONA_COMUN
                ? TAREAS_ZONA_COMUN
                : TAREAS_POR_TIPO;
        List<String> tareas = mapa.getOrDefault(tipo, mapa.get(TipoIncidencia.OTRO));
        for (int i = 0; i < tareas.size(); i++) {
            incidencia.getTareas().add(IncidenciaTarea.builder()
                    .incidencia(incidencia)
                    .descripcion(tareas.get(i))
                    .orden(i + 1)
                    .build());
        }
    }

    private void marcarPrimeraTareaDeIngreso(Incidencia incidencia) {
        incidencia.getTareas().stream()
                .filter(t -> t.getDescripcion().toLowerCase().contains("ingresar")
                        || t.getDescripcion().toLowerCase().contains("entregar")
                        || t.getOrden() == 2)
                .findFirst()
                .ifPresent(t -> {
                    t.setCompletada(true);
                    t.setFechaHoraCompletado(LocalDateTime.now());
                });
    }

    private String tituloParaTipo(TipoIncidencia tipo, AlcanceIncidencia alcance, String referencia) {
        if (alcance == AlcanceIncidencia.ZONA_COMUN) {
            return switch (tipo) {
                case MANTENIMIENTO -> "Mantenimiento — " + referencia;
                case OTRO -> "Servicio — " + referencia;
                default -> "Incidencia — " + referencia;
            };
        }
        return switch (tipo) {
            case LIMPIEZA -> "Limpieza " + referencia;
            case MANTENIMIENTO -> "Mantenimiento " + referencia;
            case SERVICIO_CUARTO -> "Servicio al cuarto — " + referencia;
            case OTRO -> "Servicio " + referencia;
        };
    }

    private String descripcionPorDefecto(TipoIncidencia tipo, AlcanceIncidencia alcance) {
        if (alcance == AlcanceIncidencia.ZONA_COMUN) {
            return switch (tipo) {
                case MANTENIMIENTO -> "Mantenimiento o reparación en zona común del hotel.";
                case OTRO -> "Servicio adicional en área común.";
                default -> "Incidencia en zona común.";
            };
        }
        return switch (tipo) {
            case LIMPIEZA -> "Limpieza post check-out o preparación de habitación.";
            case MANTENIMIENTO -> "Mantenimiento programado; inhabilita la habitación solo el día indicado.";
            case SERVICIO_CUARTO -> "Entrega de comida o amenities al huésped.";
            case OTRO -> "Servicio adicional en habitación.";
        };
    }
}
