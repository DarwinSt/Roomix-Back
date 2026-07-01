package com.example.roomix.reserva.config;

import com.example.roomix.reserva.service.TarifaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class TarifaSeed implements ApplicationRunner {

    private final TarifaService tarifaService;

    @Override
    public void run(ApplicationArguments args) {
        tarifaService.asegurarTarifasPorDefecto();
        log.info("Tarifas por tipo de habitación verificadas");
    }
}
