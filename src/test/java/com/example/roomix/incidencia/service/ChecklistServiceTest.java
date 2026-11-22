package com.example.roomix.incidencia.service;

import com.example.roomix.incidencia.domain.PlantillaChecklist;
import com.example.roomix.incidencia.domain.TipoChecklist;
import com.example.roomix.incidencia.dto.ChecklistDtos.ChecklistRequest;
import com.example.roomix.incidencia.exception.IncidenciaException;
import com.example.roomix.incidencia.repository.PlantillaChecklistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Checklists que define el administrador: predeterminadas, limpieza del texto y tareas repetidas. */
@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private PlantillaChecklistRepository repository;
    @InjectMocks
    private ChecklistService service;

    @Test
    void sinCambiosDelHotelUsaLasTareasPredeterminadas() {
        when(repository.findById(TipoChecklist.LIMPIEZA_URGENCIA)).thenReturn(Optional.empty());
        assertThat(service.tareas(TipoChecklist.LIMPIEZA_URGENCIA)).isEqualTo(TipoChecklist.LIMPIEZA_URGENCIA.getPredeterminadas());
    }

    @Test
    void usaLasTareasDelHotelSiLasDefinio() {
        PlantillaChecklist propia = PlantillaChecklist.builder().tipo(TipoChecklist.LIMPIEZA_URGENCIA)
                .tareas(new java.util.ArrayList<>(List.of("Secar el derrame"))).build();
        when(repository.findById(TipoChecklist.LIMPIEZA_URGENCIA)).thenReturn(Optional.of(propia));
        assertThat(service.tareas(TipoChecklist.LIMPIEZA_URGENCIA)).containsExactly("Secar el derrame");
    }

    @Test
    void guardaLasTareasSinEspaciosSobrantes() {
        when(repository.findById(TipoChecklist.MANTENIMIENTO_ZONA_COMUN)).thenReturn(Optional.empty());
        service.actualizar(TipoChecklist.MANTENIMIENTO_ZONA_COMUN, new ChecklistRequest(List.of("  Aislar   el área ", "Medir el cloro")));

        ArgumentCaptor<PlantillaChecklist> guardada = ArgumentCaptor.forClass(PlantillaChecklist.class);
        verify(repository).saveAndFlush(guardada.capture());
        assertThat(guardada.getValue().getTareas()).containsExactly("Aislar el área", "Medir el cloro");
    }

    @Test
    void rechazaTareasRepetidasAunqueCambienMayusculas() {
        assertThatThrownBy(() -> service.actualizar(TipoChecklist.MANTENIMIENTO_ZONA_COMUN,
                new ChecklistRequest(List.of("Revisar", "revisar"))))
                .isInstanceOf(IncidenciaException.class).extracting("errorCode").isEqualTo("INC-017");
        verify(repository, never()).saveAndFlush(any());
    }
}
