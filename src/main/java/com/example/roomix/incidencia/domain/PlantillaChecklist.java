package com.example.roomix.incidencia.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Tareas que definió el administrador para un tipo de servicio; se copian a cada incidencia nueva de ese tipo. */
@Entity
@Table(name = "plantillas_checklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaChecklist {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TipoChecklist tipo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plantilla_checklist_tareas", joinColumns = @JoinColumn(name = "tipo"))
    @OrderColumn(name = "orden")
    @Column(name = "descripcion", nullable = false, length = 150)
    @Builder.Default
    private List<String> tareas = new ArrayList<>();

    @Column(name = "actualizada_por", length = 80)
    private String actualizadaPor;

    @Column(name = "fecha_hora_actualizacion")
    private LocalDateTime fechaHoraActualizacion;
}
