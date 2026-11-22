package com.example.roomix.config;

import com.example.roomix.auth.service.CorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Configuración del hotel: IVA opcional y la copia en memoria que evita leer la base (y el logo) en cada cálculo. */
@ExtendWith(MockitoExtension.class)
class ConfiguracionHotelServiceTest {

    @Mock
    private ConfiguracionHotelRepository repository;
    @Mock
    private CorreoService correo;

    private ConfiguracionHotelService service;

    @BeforeEach
    void crear() {
        HotelProperties inicial = new HotelProperties("Hotel Roomix", null, null, null, null, null, "FV", new BigDecimal("19"), true);
        service = new ConfiguracionHotelService(repository, inicial, new Licencia("", ""), correo);
    }

    private static ConfiguracionHotel hotel(boolean cobraIva) {
        return ConfiguracionHotel.builder().id(ConfiguracionHotel.ID).nombre("Hotel Mi Kinta").cobraIva(cobraIva)
                .ivaPorcentaje(new BigDecimal("19")).exentoIvaExtranjeros(true).build();
    }

    @Test
    void siElHotelNoCobraIvaLasFacturasVanEnCeroYSinExencion() {
        when(repository.findById(ConfiguracionHotel.ID)).thenReturn(Optional.of(hotel(false)));
        HotelProperties h = service.hotel();
        assertThat(h.ivaPorcentaje()).isEqualByComparingTo("0");
        assertThat(h.exentoIvaExtranjeros()).isFalse();
    }

    @Test
    void siCobraIvaUsaSuPorcentaje() {
        when(repository.findById(ConfiguracionHotel.ID)).thenReturn(Optional.of(hotel(true)));
        assertThat(service.hotel().ivaPorcentaje()).isEqualByComparingTo("19");
        assertThat(service.hotel().exentoIvaExtranjeros()).isTrue();
    }

    @Test
    void laConfiguracionSeLeeUnaVezYSeRenuevaAlGuardar() {
        when(repository.findById(ConfiguracionHotel.ID)).thenReturn(Optional.of(hotel(false)));
        service.hotel();
        service.hotel();
        service.cobroPorPersona();
        verify(repository, times(1)).findById(ConfiguracionHotel.ID);

        ConfiguracionHotel guardada = hotel(false);
        guardada.setNombre("Hotel Renovado");
        when(repository.saveAndFlush(any())).thenReturn(guardada);
        service.actualizarApariencia(new ConfiguracionDtos.AparienciaRequest("#1E3A5F", null, null));
        assertThat(service.hotel().nombre()).isEqualTo("Hotel Renovado");
    }
}
