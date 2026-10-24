package com.example.roomix.evento.service;

import com.example.roomix.evento.domain.Evento;
import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoAcceso;
import com.example.roomix.evento.domain.EventoEnums.TipoEvento;
import com.example.roomix.evento.domain.EventoParticipante;
import com.example.roomix.evento.domain.Salon;
import com.example.roomix.evento.dto.EventoDtos.CambiarEstadoRequest;
import com.example.roomix.evento.dto.EventoDtos.EventoRequest;
import com.example.roomix.evento.dto.EventoDtos.ParticipanteRequest;
import com.example.roomix.evento.exception.EventoErrorCode;
import com.example.roomix.evento.exception.EventoException;
import com.example.roomix.evento.repository.EventoRepository;
import com.example.roomix.evento.repository.SalonRepository;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.huesped.service.HuespedService;
import com.example.roomix.personal.service.PersonalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;
    @Mock
    private SalonRepository salonRepository;
    @Mock
    private PersonalService personalService;
    @Mock
    private HuespedService huespedService;
    @Mock
    private FacturaRepository facturaRepository;

    @InjectMocks
    private EventoService eventoService;

    private final Salon salon = Salon.builder().id(1L).nombre("Salón Bocagrande").capacidad(60)
            .tarifaHora(BigDecimal.TEN).activo(true).build();

    private EventoRequest solicitud(int capacidad, LocalDateTime inicio, LocalDateTime fin) {
        return new EventoRequest("Lanzamiento", null, TipoEvento.CORPORATIVO, TipoAcceso.PRIVADO, inicio, fin, 1L, null,
                capacidad, "Empresa", null, null, null, new BigDecimal("1000000"), null, List.of());
    }

    @Test
    void rechazaEventoCuandoElSalonYaEstaOcupado() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(eventoRepository.existeCruce(eq(1L), any(), any(), anyLong(), any())).thenReturn(true);

        assertThatThrownBy(() -> eventoService.crear(solicitud(20, inicio, inicio.plusHours(3))))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.SALON_OCUPADO));
    }

    @Test
    void rechazaCapacidadMayorALaDelSalon() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        assertThatThrownBy(() -> eventoService.crear(solicitud(61, inicio, inicio.plusHours(3))))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.CAPACIDAD_SALON));
    }

    @Test
    void rechazaFinAnteriorAlInicio() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(3);
        assertThatThrownBy(() -> eventoService.crear(solicitud(10, inicio, inicio.minusHours(1))))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.FECHAS_INVALIDAS));
    }

    @Test
    void noAdmiteMasParticipantesQueLaCapacidad() {
        Evento evento = evento(1, TipoAcceso.PUBLICO);
        evento.getParticipantes().add(EventoParticipante.builder().id(1L).evento(evento).nombre("Uno")
                .fechaHoraRegistro(LocalDateTime.now()).build());
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.agregarParticipante(5L, new ParticipanteRequest(null, "Dos", null, null, null, true)))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.CUPO_LLENO));
    }

    @Test
    void eventoExclusivoParaHuespedesExigeHuespedRegistrado() {
        Evento evento = evento(10, TipoAcceso.SOLO_HUESPEDES);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.agregarParticipante(5L, new ParticipanteRequest(null, "Externo", null, null, null, true)))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.SOLO_HUESPEDES));
    }

    @Test
    void noSePuedeFinalizarUnEventoQueNoHaIniciado() {
        Evento evento = evento(10, TipoAcceso.PUBLICO);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.cambiarEstado(5L, new CambiarEstadoRequest(EstadoEvento.FINALIZADO, null)))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.TRANSICION_INVALIDA));
    }

    @Test
    void cancelarExigeMotivo() {
        Evento evento = evento(10, TipoAcceso.PUBLICO);
        when(eventoRepository.findById(5L)).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> eventoService.cambiarEstado(5L, new CambiarEstadoRequest(EstadoEvento.CANCELADO, " ")))
                .isInstanceOfSatisfying(EventoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(EventoErrorCode.MOTIVO_REQUERIDO));
    }

    private Evento evento(int capacidad, TipoAcceso acceso) {
        Evento evento = Evento.builder()
                .id(5L).nombre("Evento").tipo(TipoEvento.SOCIAL).tipoAcceso(acceso).estado(EstadoEvento.PROGRAMADO)
                .fechaHoraInicio(LocalDateTime.now().plusDays(2)).fechaHoraFin(LocalDateTime.now().plusDays(2).plusHours(3))
                .salon(salon).capacidad(capacidad).organizadorNombre("Org").valorBase(BigDecimal.ZERO).creadoPor("eventos")
                .build();
        lenient().when(eventoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        return evento;
    }
}
