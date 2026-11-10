package com.example.roomix.personal.config;

import com.example.roomix.personal.domain.Personal;
import com.example.roomix.personal.repository.PersonalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Datos de ejemplo: solo en modo demostración (APP_SEED_ENABLED=true), nunca en la base de un hotel real. */
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class PersonalSeed implements ApplicationRunner {

    private final PersonalRepository personalRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (personalRepository.count() > 0) {
            return;
        }
        personalRepository.save(Personal.builder()
                .nombre("María López")
                .rol("Housekeeping")
                .departamento("Limpieza")
                .build());
        personalRepository.save(Personal.builder()
                .nombre("Juan Pérez")
                .rol("Housekeeping")
                .departamento("Limpieza")
                .build());
        personalRepository.save(Personal.builder()
                .nombre("Ana García")
                .rol("Supervisora")
                .departamento("Limpieza")
                .build());
    }
}
