package com.example.roomix.incidencia.repository;

import com.example.roomix.incidencia.domain.PlantillaChecklist;
import com.example.roomix.incidencia.domain.TipoChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaChecklistRepository extends JpaRepository<PlantillaChecklist, TipoChecklist> {
}
