package com.example.roomix.personal.service;

import com.example.roomix.common.exception.ModuloException;
import com.example.roomix.personal.dto.PersonalRequest;
import com.example.roomix.personal.exception.PersonalErrorCode;

import com.example.roomix.incidencia.domain.EstadoIncidencia;
import com.example.roomix.incidencia.repository.IncidenciaRepository;
import com.example.roomix.personal.domain.Personal;
import com.example.roomix.personal.dto.PersonalResponse;
import com.example.roomix.personal.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalService {

    private static final List<EstadoIncidencia> ESTADOS_PERSONAL_OCUPADO = List.of(
            EstadoIncidencia.ASIGNADA,
            EstadoIncidencia.EN_PROGRESO
    );

    private final PersonalRepository personalRepository;
    private final IncidenciaRepository incidenciaRepository;

    public List<PersonalResponse> listar(Boolean activo) {
        List<Personal> personal = activo == null
                ? personalRepository.findAllByOrderByNombreAsc()
                : activo
                        ? personalRepository.findByActivoTrueOrderByNombreAsc()
                        : personalRepository.findAllByOrderByNombreAsc().stream()
                                .filter(p -> !p.isActivo())
                                .toList();

        Set<Long> ocupados = incidenciaRepository.findPersonalIdsOcupados(ESTADOS_PERSONAL_OCUPADO);

        return personal.stream()
                .map(p -> PersonalResponse.from(p, ocupados.contains(p.getId())))
                .toList();
    }

    @Transactional
    public PersonalResponse crear(PersonalRequest request) {
        String nombre = request.nombre().trim();
        if (personalRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ModuloException("PERSONAL", PersonalErrorCode.NOMBRE_DUPLICADO, nombre);
        }
        Personal p = personalRepository.save(Personal.builder()
                .nombre(nombre)
                .rol(request.rol().trim())
                .departamento(vacioANull(request.departamento()))
                .activo(request.activo() == null || request.activo())
                .build());
        return PersonalResponse.from(p, false);
    }

    @Transactional
    public PersonalResponse actualizar(Long id, PersonalRequest request) {
        Personal p = personalRepository.findById(id)
                .orElseThrow(() -> new ModuloException("PERSONAL", PersonalErrorCode.PERSONAL_NO_ENCONTRADO, id));
        String nombre = request.nombre().trim();
        if (personalRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ModuloException("PERSONAL", PersonalErrorCode.NOMBRE_DUPLICADO, nombre);
        }
        p.setNombre(nombre);
        p.setRol(request.rol().trim());
        p.setDepartamento(vacioANull(request.departamento()));
        if (request.activo() != null) {
            validarDesactivacion(p, request.activo());
            p.setActivo(request.activo());
        }
        return PersonalResponse.from(p, estaOcupado(p.getId()));
    }

    @Transactional
    public PersonalResponse cambiarActivo(Long id, boolean activo) {
        Personal p = personalRepository.findById(id)
                .orElseThrow(() -> new ModuloException("PERSONAL", PersonalErrorCode.PERSONAL_NO_ENCONTRADO, id));
        validarDesactivacion(p, activo);
        p.setActivo(activo);
        return PersonalResponse.from(p, estaOcupado(p.getId()));
    }

    private void validarDesactivacion(Personal p, boolean activo) {
        if (!activo && p.isActivo() && estaOcupado(p.getId())) {
            throw new ModuloException("PERSONAL", PersonalErrorCode.PERSONAL_OCUPADO, p.getNombre());
        }
    }

    private boolean estaOcupado(Long id) {
        return incidenciaRepository.findPersonalIdsOcupados(ESTADOS_PERSONAL_OCUPADO).contains(id);
    }

    private static String vacioANull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public Personal buscarEntidad(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new com.example.roomix.incidencia.exception.IncidenciaException(
                        com.example.roomix.incidencia.exception.IncidenciaErrorCode.PERSONAL_NO_ENCONTRADO,
                        id
                ));
    }
}
