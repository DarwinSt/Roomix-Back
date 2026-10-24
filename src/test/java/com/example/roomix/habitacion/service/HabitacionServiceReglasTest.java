package com.example.roomix.habitacion.service;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HabitacionServiceReglasTest {

    @Test
    void elPisoSeDeduceDelNumeroSiNoSeIndica() {
        assertThat(HabitacionService.resolverPiso(null, "205")).isEqualTo(2);
        assertThat(HabitacionService.resolverPiso(null, "401A")).isEqualTo(4);
        assertThat(HabitacionService.resolverPiso(null, "PH")).isEqualTo(1);
        assertThat(HabitacionService.resolverPiso(7, "205")).isEqualTo(7);
    }

    @Test
    void laCapacidadPorDefectoDependeDelTipo() {
        assertThat(HabitacionService.resolverCapacidad(null, TipoHabitacion.INDIVIDUAL)).isEqualTo(1);
        assertThat(HabitacionService.resolverCapacidad(null, TipoHabitacion.FAMILIAR)).isEqualTo(5);
        assertThat(HabitacionService.resolverCapacidad(4, TipoHabitacion.DOBLE)).isEqualTo(4);
    }
}
