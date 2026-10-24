package com.example.roomix.auth.service;

import com.example.roomix.auth.exception.AuthErrorCode;
import com.example.roomix.auth.exception.AuthException;

import java.security.SecureRandom;

/**
 * Reglas de contraseñas: mínimo 8 caracteres con mayúscula, minúscula y número.
 */
public final class PasswordPolicy {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String SIMBOLOS = "*#$%&@";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordPolicy() {
    }

    public static void validar(String password) {
        if (password == null
                || password.length() < 8
                || password.length() > 72
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")) {
            throw new AuthException(AuthErrorCode.PASSWORD_DEBIL);
        }
    }

    /** Contraseña temporal aleatoria que cumple la política (12 caracteres, sin caracteres ambiguos). */
    public static String generarTemporal() {
        StringBuilder sb = new StringBuilder();
        sb.append(caracter(MAYUSCULAS)).append(caracter(MINUSCULAS)).append(caracter(NUMEROS)).append(caracter(SIMBOLOS));
        String todos = MAYUSCULAS + MINUSCULAS + NUMEROS;
        while (sb.length() < 12) {
            sb.append(caracter(todos));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = chars[i];
            chars[i] = chars[j];
            chars[j] = t;
        }
        return new String(chars);
    }

    private static char caracter(String fuente) {
        return fuente.charAt(RANDOM.nextInt(fuente.length()));
    }
}
