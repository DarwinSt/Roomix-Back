package com.example.roomix.personal.service;

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

    public Personal buscarEntidad(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new com.example.roomix.incidencia.exception.IncidenciaException(
                        com.example.roomix.incidencia.exception.IncidenciaErrorCode.PERSONAL_NO_ENCONTRADO,
                        id
                ));
    }
}
