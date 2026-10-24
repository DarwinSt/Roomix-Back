package com.example.roomix.auth.service;

import com.example.roomix.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"corta1A", "sinmayuscula1", "SINMINUSCULA1", "SinNumeros", ""})
    void rechazaContrasenasDebiles(String password) {
        assertThatThrownBy(() -> PasswordPolicy.validar(password)).isInstanceOf(AuthException.class);
    }

    @Test
    void rechazaNula() {
        assertThatThrownBy(() -> PasswordPolicy.validar(null)).isInstanceOf(AuthException.class);
    }

    @Test
    void aceptaContrasenaQueCumpleLaPolitica() {
        assertThatCode(() -> PasswordPolicy.validar("Roomix2026*")).doesNotThrowAnyException();
    }

    @Test
    void laContrasenaTemporalCumpleLaPoliticaYEsAleatoria() {
        String a = PasswordPolicy.generarTemporal();
        String b = PasswordPolicy.generarTemporal();
        assertThat(a).hasSize(12);
        assertThatCode(() -> PasswordPolicy.validar(a)).doesNotThrowAnyException();
        assertThat(a).isNotEqualTo(b);
    }
}
