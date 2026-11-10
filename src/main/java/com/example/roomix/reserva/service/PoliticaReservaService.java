package com.example.roomix.reserva.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.reserva.domain.PoliticaReserva;
import com.example.roomix.reserva.dto.PromocionDtos.PoliticaReservaRequest;
import com.example.roomix.reserva.dto.PromocionDtos.PoliticaReservaResponse;
import com.example.roomix.reserva.repository.PoliticaReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

/** Abono mínimo para reservar y penalidad por cancelación, configurados por el administrador. */
@Service
@RequiredArgsConstructor
public class PoliticaReservaService {

    private final PoliticaReservaRepository repository;

    @Transactional
    public PoliticaReserva vigente() {
        return repository.findById(PoliticaReserva.ID)
                .orElseGet(() -> repository.save(PoliticaReserva.builder().id(PoliticaReserva.ID).build()));
    }

    @Transactional
    public PoliticaReservaResponse obtener() {
        return PoliticaReservaResponse.from(vigente());
    }

    @Transactional
    public PoliticaReservaResponse actualizar(PoliticaReservaRequest request) {
        PoliticaReserva politica = vigente();
        politica.setAbonoMinimoPct(request.abonoMinimoPct().setScale(2, RoundingMode.HALF_UP));
        politica.setPenalidadCancelacionPct(request.penalidadCancelacionPct().setScale(2, RoundingMode.HALF_UP));
        politica.setActualizadaPor(UsuarioActual.username());
        return PoliticaReservaResponse.from(repository.saveAndFlush(politica));
    }
}
