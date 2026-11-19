package com.example.roomix.config;

import com.example.roomix.common.exception.ModuloException;

public class ConfiguracionException extends ModuloException {

    public ConfiguracionException(ConfiguracionErrorCode codigo, Object... args) {
        super("CONFIGURACION", codigo, args);
    }
}
