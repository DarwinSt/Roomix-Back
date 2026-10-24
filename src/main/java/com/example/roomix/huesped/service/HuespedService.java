package com.example.roomix.huesped.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;

import com.example.roomix.habitacion.domain.EstadoHabitacion;
import com.example.roomix.habitacion.domain.Habitacion;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.dto.HuespedRequest;
import com.example.roomix.huesped.dto.HuespedResponse;
import com.example.roomix.huesped.exception.HuespedErrorCode;
import com.example.roomix.huesped.exception.HuespedException;
import com.example.roomix.huesped.repository.HuespedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HuespedService {

    private static final EnumSet<EstadoHabitacion> ESTADOS_CON_HUESPED = EnumSet.of(
            EstadoHabitacion.RESERVADO,
            EstadoHabitacion.OCUPADO
    );

    private final HuespedRepository huespedRepository;
    private final HabitacionRepository habitacionRepository;

    public List<HuespedResponse> listar(Boolean activo, String busqueda) {
        String termino = busqueda != null ? busqueda.trim() : null;
        List<Huesped> huespedes = huespedRepository.buscar(activo, termino);
        Map<Long, String> habitacionPorHuesped = mapaHabitacionActual();
        return huespedes.stream()
                .map(h -> HuespedResponse.from(h, habitacionPorHuesped.get(h.getId())))
                .toList();
    }

    /**
     * Búsqueda rápida para autocompletar: cada palabra debe aparecer en nombre, apellidos, documento o correo,
     * sin distinguir mayúsculas ni tildes ("steween mora" o "gomez" encuentran "Steween Mora" y "Gómez").
     */
    public List<HuespedResponse> buscarRapido(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String[] palabras = normalizar(texto).split("\\s+");
        Specification<Huesped> spec = (root, query, cb) -> {
            Expression<String> campos = cb.concat(cb.concat(cb.concat(cb.concat(cb.concat(cb.concat(
                    root.get("nombre"), " "), root.get("apellidos")), " "), root.get("numeroDocumento")), " "),
                    cb.coalesce(root.get("email"), ""));
            Expression<String> sinTildes = cb.function("translate", String.class, cb.lower(campos),
                    cb.literal(CON_TILDE), cb.literal(SIN_TILDE));
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isTrue(root.get("activo")));
            for (int i = 0; i < Math.min(palabras.length, 5); i++) {
                condiciones.add(cb.like(sinTildes, "%" + palabras[i] + "%"));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        int tope = Math.min(Math.max(limite, 1), 20);
        List<Huesped> encontrados = huespedRepository.findAll(spec,
                PageRequest.of(0, tope, Sort.by("apellidos").ascending().and(Sort.by("nombre").ascending()))).getContent();
        Map<Long, String> habitacionPorHuesped = mapaHabitacionActual();
        return encontrados.stream()
                .map(h -> HuespedResponse.from(h, habitacionPorHuesped.get(h.getId())))
                .toList();
    }

    private static final String CON_TILDE = "áéíóúüàèìòùñ";
    private static final String SIN_TILDE = "aeiouuaeioun";

    private static String normalizar(String texto) {
        String t = texto.trim().toLowerCase();
        StringBuilder sb = new StringBuilder(t.length());
        for (char c : t.toCharArray()) {
            int i = CON_TILDE.indexOf(c);
            sb.append(i >= 0 ? SIN_TILDE.charAt(i) : c);
        }
        return sb.toString();
    }

    public HuespedResponse obtenerPorId(Long id) {
        Huesped huesped = buscarEntidad(id);
        return HuespedResponse.from(huesped, habitacionActualNumero(huesped.getId()));
    }

    @Transactional
    public HuespedResponse crear(HuespedRequest request) {
        validarDocumentoUnico(request.numeroDocumento(), null);
        Huesped huesped = aplicarRequest(Huesped.builder().activo(true).build(), request);
        return HuespedResponse.from(huespedRepository.save(huesped), null);
    }

    @Transactional
    public HuespedResponse actualizar(Long id, HuespedRequest request) {
        Huesped huesped = buscarEntidad(id);
        validarDocumentoUnico(request.numeroDocumento(), id);
        aplicarRequest(huesped, request);
        return HuespedResponse.from(
                huespedRepository.save(huesped),
                habitacionActualNumero(huesped.getId())
        );
    }

    @Transactional
    public void eliminar(Long id) {
        Huesped huesped = buscarEntidad(id);
        huesped.setActivo(false);
        huespedRepository.save(huesped);
    }

    public Huesped buscarActivo(Long id) {
        Huesped huesped = buscarEntidad(id);
        if (!huesped.isActivo()) {
            throw new HuespedException(HuespedErrorCode.HUESPED_INACTIVO, huesped.nombreCompleto());
        }
        return huesped;
    }

    public Huesped buscarEntidad(Long id) {
        return huespedRepository.findById(id)
                .orElseThrow(() -> new HuespedException(HuespedErrorCode.HUESPED_NO_ENCONTRADO, id));
    }

    public void validarDisponibleParaAsignar(Huesped huesped, Long habitacionIdExcluida) {
        habitacionRepository
                .findByHuespedIdAndEstadoIn(huesped.getId(), ESTADOS_CON_HUESPED)
                .filter(h -> !h.getId().equals(habitacionIdExcluida))
                .ifPresent(h -> {
                    throw new HuespedException(HuespedErrorCode.HUESPED_YA_ASIGNADO, h.getNumero());
                });
    }

    private Huesped aplicarRequest(Huesped huesped, HuespedRequest request) {
        huesped.setNombre(request.nombre().trim());
        huesped.setApellidos(request.apellidos().trim());
        huesped.setTipoDocumento(request.tipoDocumento());
        huesped.setNumeroDocumento(request.numeroDocumento().trim().toUpperCase());
        huesped.setEmail(request.email().trim().toLowerCase());
        huesped.setTelefono(request.telefono().trim());
        huesped.setNacionalidad(normalizarOpcional(request.nacionalidad()));
        huesped.setFechaNacimiento(request.fechaNacimiento());
        huesped.setNotas(normalizarOpcional(request.notas()));
        if (request.activo() != null) {
            huesped.setActivo(request.activo());
        }
        if (Boolean.TRUE.equals(request.autorizaTratamientoDatos()) && !huesped.isAutorizaTratamientoDatos()) {
            huesped.setAutorizaTratamientoDatos(true);
            huesped.setFechaHoraAutorizacion(java.time.LocalDateTime.now());
        }
        return huesped;
    }

    private void validarDocumentoUnico(String numeroDocumento, Long idExcluido) {
        String doc = numeroDocumento.trim().toUpperCase();
        if (idExcluido == null) {
            huespedRepository.findByNumeroDocumentoIgnoreCase(doc).ifPresent(h -> {
                throw new HuespedException(HuespedErrorCode.DOCUMENTO_DUPLICADO, doc);
            });
            return;
        }
        if (huespedRepository.existsByNumeroDocumentoIgnoreCaseAndIdNot(doc, idExcluido)) {
            throw new HuespedException(HuespedErrorCode.DOCUMENTO_DUPLICADO, doc);
        }
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private Map<Long, String> mapaHabitacionActual() {
        return habitacionRepository.findByEstadoIn(ESTADOS_CON_HUESPED).stream()
                .filter(h -> h.getHuesped() != null)
                .collect(Collectors.toMap(
                        h -> h.getHuesped().getId(),
                        Habitacion::getNumero,
                        (a, b) -> a
                ));
    }

    private String habitacionActualNumero(Long huespedId) {
        return habitacionRepository
                .findByHuespedIdAndEstadoIn(huespedId, ESTADOS_CON_HUESPED)
                .map(Habitacion::getNumero)
                .orElse(null);
    }
}
