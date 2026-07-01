package com.example.roomix.reserva.domain;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarifas_tipo_habitacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaTipoHabitacion {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_habitacion", nullable = false, length = 30)
    private TipoHabitacion tipoHabitacion;

    @Column(name = "precio_noche", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioNoche;

    @UpdateTimestamp
    @Column(name = "fecha_hora_ultima_actualizacion", nullable = false)
    private LocalDateTime fechaHoraUltimaActualizacion;
}
