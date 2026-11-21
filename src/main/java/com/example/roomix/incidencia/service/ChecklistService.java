package com.example.roomix.incidencia.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.incidencia.domain.PlantillaChecklist;
import com.example.roomix.incidencia.domain.TipoChecklist;
import com.example.roomix.incidencia.dto.ChecklistDtos.ChecklistRequest;
import com.example.roomix.incidencia.dto.ChecklistDtos.ChecklistResponse;
import com.example.roomix.incidencia.exception.IncidenciaErrorCode;
import com.example.roomix.incidencia.exception.IncidenciaException;
import com.example.roomix.incidencia.repository.PlantillaChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Checklists de los servicios (qué acciones de aseo y mantenimiento se hacen). Los define el administrador; las
 * incidencias copian las tareas al crearse, así que un cambio solo aplica a los servicios nuevos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChecklistService {

    private final PlantillaChecklistRepository repository;

    public List<ChecklistResponse> listar() {
        return Arrays.stream(TipoChecklist.values()).map(this::respuesta).toList();
    }

    /** Tareas vigentes para un tipo: las del administrador o, si no las cambió, las predeterminadas. */
    public List<String> tareas(TipoChecklist tipo) {
        return repository.findById(tipo)
                .map(p -> List.copyOf(p.getTareas()))
                .filter(l -> !l.isEmpty())
                .orElse(tipo.getPredeterminadas());
    }

    @Transactional
    public ChecklistResponse actualizar(TipoChecklist tipo, ChecklistRequest request) {
        List<String> tareas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (String t : request.tareas()) {
            String limpia = t.trim().replaceAll("\\s+", " ");
            if (!vistas.add(limpia.toLowerCase(Locale.ROOT))) {
                throw new IncidenciaException(IncidenciaErrorCode.CHECKLIST_TAREA_REPETIDA, limpia);
            }
            tareas.add(limpia);
        }
        PlantillaChecklist plantilla = repository.findById(tipo)
                .orElseGet(() -> PlantillaChecklist.builder().tipo(tipo).build());
        plantilla.getTareas().clear();
        plantilla.getTareas().addAll(tareas);
        plantilla.setActualizadaPor(UsuarioActual.username());
        plantilla.setFechaHoraActualizacion(LocalDateTime.now());
        repository.saveAndFlush(plantilla);
        return respuesta(tipo);
    }

    /** Vuelve a las tareas predeterminadas de Roomix. */
    @Transactional
    public ChecklistResponse restaurar(TipoChecklist tipo) {
        repository.findById(tipo).ifPresent(repository::delete);
        repository.flush();
        return respuesta(tipo);
    }

    private ChecklistResponse respuesta(TipoChecklist tipo) {
        boolean personalizada = repository.findById(tipo).filter(p -> !p.getTareas().isEmpty()).isPresent();
        return new ChecklistResponse(tipo, tipo.getGrupo(), tipo.getNombre(), tipo.getDescripcion(), tareas(tipo),
                personalizada, tipo.getPredeterminadas());
    }
}
