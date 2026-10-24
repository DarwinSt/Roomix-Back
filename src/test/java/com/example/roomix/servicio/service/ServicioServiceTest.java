package com.example.roomix.servicio.service;

import com.example.roomix.finanzas.domain.OrigenItem;
import com.example.roomix.finanzas.repository.FacturaItemRepository;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.personal.service.PersonalService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.domain.SolicitudServicio;
import com.example.roomix.servicio.dto.ServicioDtos.CambiarEstadoRequest;
import com.example.roomix.servicio.dto.ServicioDtos.SolicitudRequest;
import com.example.roomix.servicio.exception.ServicioErrorCode;
import com.example.roomix.servicio.exception.ServicioException;
import com.example.roomix.servicio.repository.ServicioCatalogoRepository;
import com.example.roomix.servicio.repository.SolicitudServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioCatalogoRepository catalogoRepository;
    @Mock
    private SolicitudServicioRepository solicitudRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PersonalService personalService;
    @Mock
    private FacturaItemRepository facturaItemRepository;

    @InjectMocks
    private ServicioService servicioService;

    @Test
    void noSePidenServiciosParaReservasFinalizadas() {
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(
                Reserva.builder().id(1L).estadoReserva(EstadoReserva.FINALIZADA).build()));

        assertThatThrownBy(() -> servicioService.crearSolicitud(new SolicitudRequest(1L, 2L, 1, null, null, null)))
                .isInstanceOfSatisfying(ServicioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ServicioErrorCode.RESERVA_NO_ACTIVA));
    }

    @Test
    void unaSolicitudCompletadaNoVuelveAEnProceso() {
        when(solicitudRepository.findById(9L)).thenReturn(Optional.of(
                SolicitudServicio.builder().id(9L).estado(EstadoSolicitud.COMPLETADA).build()));

        assertThatThrownBy(() -> servicioService.cambiarEstado(9L, new CambiarEstadoRequest(EstadoSolicitud.EN_PROCESO, null)))
                .isInstanceOfSatisfying(ServicioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ServicioErrorCode.TRANSICION_INVALIDA));
    }

    @Test
    void cancelarExigeMotivoYNoPuedeEstarFacturada() {
        when(solicitudRepository.findById(9L)).thenReturn(Optional.of(
                SolicitudServicio.builder().id(9L).estado(EstadoSolicitud.SOLICITADA).build()));

        assertThatThrownBy(() -> servicioService.cambiarEstado(9L, new CambiarEstadoRequest(EstadoSolicitud.CANCELADA, "")))
                .isInstanceOfSatisfying(ServicioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ServicioErrorCode.MOTIVO_REQUERIDO));

        when(facturaItemRepository.estaFacturado(OrigenItem.SERVICIO, 9L)).thenReturn(true);
        assertThatThrownBy(() -> servicioService.cambiarEstado(9L, new CambiarEstadoRequest(EstadoSolicitud.CANCELADA, "No lo quiso")))
                .isInstanceOfSatisfying(ServicioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(ServicioErrorCode.SOLICITUD_FACTURADA));
    }
}
