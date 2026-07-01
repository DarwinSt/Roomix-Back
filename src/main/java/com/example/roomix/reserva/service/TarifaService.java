package com.example.roomix.reserva.service;

import com.example.roomix.habitacion.domain.TipoHabitacion;
import com.example.roomix.reserva.domain.TarifaTipoHabitacion;
import com.example.roomix.reserva.dto.ActualizarTarifaRequest;
import com.example.roomix.reserva.dto.TarifaTipoResponse;
import com.example.roomix.reserva.repository.TarifaTipoHabitacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarifaService {

    private final TarifaTipoHabitacionRepository tarifaRepository;

    public List<TarifaTipoResponse> listar() {
        return tarifaRepository.findAll().stream()
                .map(TarifaTipoResponse::from)
                .toList();
    }

    @Transactional
    public BigDecimal obtenerPrecioNoche(TipoHabitacion tipo) {
        return tarifaRepository.findById(tipo)
                .map(TarifaTipoHabitacion::getPrecioNoche)
                .orElseGet(() -> tarifaRepository.save(TarifaTipoHabitacion.builder()
                        .tipoHabitacion(tipo)
                        .precioNoche(precioPorDefecto(tipo))
                        .build()).getPrecioNoche());
    }

    @Transactional
    public TarifaTipoResponse actualizar(TipoHabitacion tipo, ActualizarTarifaRequest request) {
        TarifaTipoHabitacion tarifa = tarifaRepository.findById(tipo)
                .orElseGet(() -> TarifaTipoHabitacion.builder().tipoHabitacion(tipo).build());
        tarifa.setPrecioNoche(request.precioNoche());
        return TarifaTipoResponse.from(tarifaRepository.save(tarifa));
    }

    @Transactional
    public void asegurarTarifasPorDefecto() {
        for (TipoHabitacion tipo : TipoHabitacion.values()) {
            if (!tarifaRepository.existsById(tipo)) {
                tarifaRepository.save(TarifaTipoHabitacion.builder()
                        .tipoHabitacion(tipo)
                        .precioNoche(precioPorDefecto(tipo))
                        .build());
            }
        }
    }

    private BigDecimal precioPorDefecto(TipoHabitacion tipo) {
        return switch (tipo) {
            case INDIVIDUAL -> BigDecimal.valueOf(80);
            case DOBLE -> BigDecimal.valueOf(120);
            case TRIPLE -> BigDecimal.valueOf(150);
            case SUITE -> BigDecimal.valueOf(250);
            case FAMILIAR -> BigDecimal.valueOf(180);
            case EJECUTIVA -> BigDecimal.valueOf(200);
        };
    }
}
