package com.example.roomix.common.exception;

/**
 * Excepción de negocio genérica para los módulos que usan {@link ModuloErrorCode}.
 */
public class ModuloException extends BusinessException {

    private final ModuloErrorCode codigo;
    private final String modulo;

    public ModuloException(String modulo, ModuloErrorCode codigo, Object... args) {
        super(codigo.getCode(), codigo.getHttpStatus(), codigo.formatMessage(args));
        this.codigo = codigo;
        this.modulo = modulo;
    }

    public ModuloErrorCode getCodigo() {
        return codigo;
    }

    @Override
    public String getTitulo() {
        return codigo.name();
    }

    @Override
    public String getModulo() {
        return modulo;
    }
}
