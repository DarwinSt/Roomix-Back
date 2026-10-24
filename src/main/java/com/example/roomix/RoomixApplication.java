package com.example.roomix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RoomixApplication {

    /** Zona horaria del hotel; define qué es "hoy" para reservas, mantenimientos y check-in/out. */
    private static final String ZONA_HORARIA_POR_DEFECTO = "America/Bogota";

    public static void main(String[] args) {
        // Los servidores en la nube corren en UTC: sin esto el "día" cambiaría a las 7 p. m. hora Colombia
        String zona = System.getenv().getOrDefault("APP_TIMEZONE", ZONA_HORARIA_POR_DEFECTO);
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        SpringApplication.run(RoomixApplication.class, args);
    }

}
