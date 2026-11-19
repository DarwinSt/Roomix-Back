package com.example.roomix.incidencia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Material del inventario gastado en una limpieza o un mantenimiento. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUsado {

    @Column(name = "articulo_id", nullable = false)
    private Long articuloId;

    @Column(name = "articulo_nombre", nullable = false, length = 120)
    private String articuloNombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "unidad", length = 20)
    private String unidad;
}
