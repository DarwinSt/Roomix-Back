package com.example.roomix.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Módulos contratados por el hotel. Se definen en el servidor de cada cliente con la variable LICENCIA_MODULOS
 * (por ejemplo «CAJA,INVENTARIO,REPORTES»); vacía o «TODOS» habilita todos. El hotel no puede cambiarla desde la
 * aplicación: solo quien administra el servidor.
 */
@Slf4j
@Component
public class Licencia {

    private final Set<Modulo> habilitados;
    private final String cliente;

    public Licencia(@Value("${app.licencia.modulos:}") String modulos, @Value("${app.licencia.cliente:}") String cliente) {
        this.cliente = cliente == null ? "" : cliente.trim();
        if (modulos == null || modulos.isBlank() || modulos.trim().equalsIgnoreCase("TODOS")) {
            this.habilitados = Collections.unmodifiableSet(EnumSet.allOf(Modulo.class));
        } else {
            EnumSet<Modulo> set = EnumSet.noneOf(Modulo.class);
            Arrays.stream(modulos.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(s -> {
                try {
                    set.add(Modulo.valueOf(s.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("LICENCIA_MODULOS: módulo desconocido «{}» (válidos: {})", s, Arrays.toString(Modulo.values()));
                }
            });
            this.habilitados = Collections.unmodifiableSet(set);
        }
        log.info("Licencia{}: módulos habilitados {}", this.cliente.isEmpty() ? "" : " de " + this.cliente, this.habilitados);
    }

    public boolean habilitado(Modulo modulo) {
        return habilitados.contains(modulo);
    }

    public Set<Modulo> habilitados() {
        return habilitados;
    }

    public String cliente() {
        return cliente;
    }
}
