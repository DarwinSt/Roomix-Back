package com.example.roomix.huesped.config;

import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.domain.TipoDocumento;
import com.example.roomix.huesped.repository.HuespedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class HuespedSeed implements ApplicationRunner {

    private final HuespedRepository huespedRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (huespedRepository.count() > 0) {
            return;
        }
        huespedRepository.save(Huesped.builder()
                .nombre("María")
                .apellidos("González Pérez")
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento("12345678A")
                .email("maria.gonzalez@email.com")
                .telefono("+34 600 111 222")
                .nacionalidad("España")
                .activo(true)
                .build());
        huespedRepository.save(Huesped.builder()
                .nombre("James")
                .apellidos("Miller")
                .tipoDocumento(TipoDocumento.PASAPORTE)
                .numeroDocumento("US99887766")
                .email("james.miller@email.com")
                .telefono("+1 555 010 2030")
                .nacionalidad("Estados Unidos")
                .activo(true)
                .build());
        log.info("Huéspedes de ejemplo cargados");
    }
}
