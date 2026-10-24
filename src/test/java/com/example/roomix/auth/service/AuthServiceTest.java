package com.example.roomix.auth.service;

import com.example.roomix.auditoria.service.AuditoriaService;
import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.dto.AuthDtos.LoginRequest;
import com.example.roomix.auth.dto.AuthDtos.LoginResponse;
import com.example.roomix.auth.exception.AuthErrorCode;
import com.example.roomix.auth.exception.AuthException;
import com.example.roomix.auth.repository.TokenRecuperacionRepository;
import com.example.roomix.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenRecuperacionRepository tokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private CorreoService correoService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(authService, "passwordEncoder", encoder);
        usuario = Usuario.builder()
                .id(1L)
                .username("recepcion")
                .nombreCompleto("Recepción")
                .email("recepcion@roomix.test")
                .rol(Rol.RECEPCION)
                .passwordHash(encoder.encode("Roomix2026*"))
                .activo(true)
                .build();
    }

    @Test
    void loginCorrectoEmiteTokenYReiniciaIntentos() {
        usuario.setIntentosFallidos(3);
        when(usuarioRepository.findByUsernameIgnoreCase("recepcion")).thenReturn(Optional.of(usuario));
        when(jwtService.emitir(usuario)).thenReturn(new JwtService.TokenEmitido("jwt", Instant.now()));

        LoginResponse respuesta = authService.login(new LoginRequest("recepcion", "Roomix2026*"), null);

        assertThat(respuesta.token()).isEqualTo("jwt");
        assertThat(respuesta.usuario().rol()).isEqualTo(Rol.RECEPCION);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getUltimoAcceso()).isNotNull();
    }

    @Test
    void contrasenaIncorrectaSumaIntentoYAlQuintoBloquea() {
        when(usuarioRepository.findByUsernameIgnoreCase("recepcion")).thenReturn(Optional.of(usuario));

        for (int i = 1; i < AuthService.MAX_INTENTOS; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("recepcion", "mala"), null))
                    .isInstanceOf(AuthException.class);
            assertThat(usuario.getIntentosFallidos()).isEqualTo(i);
        }
        assertThatThrownBy(() -> authService.login(new LoginRequest("recepcion", "mala"), null))
                .isInstanceOf(AuthException.class);
        assertThat(usuario.getBloqueadoHasta()).isAfter(LocalDateTime.now());

        // Bloqueada: ni siquiera la contraseña correcta permite entrar
        assertThatThrownBy(() -> authService.login(new LoginRequest("recepcion", "Roomix2026*"), null))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(AuthErrorCode.USUARIO_BLOQUEADO));
    }

    @Test
    void usuarioInactivoNoPuedeIngresar() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsernameIgnoreCase("recepcion")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("recepcion", "Roomix2026*"), null))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(AuthErrorCode.USUARIO_INACTIVO));
    }

    @Test
    void usuarioInexistenteResponde401SinRevelarloYAudita() {
        when(usuarioRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie", "x"), null))
                .isInstanceOfSatisfying(AuthException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(AuthErrorCode.CREDENCIALES_INVALIDAS));
        org.mockito.Mockito.verify(auditoriaService).registrarEvento(
                any(), any(), org.mockito.ArgumentMatchers.eq("LOGIN_FALLIDO"), any(), org.mockito.ArgumentMatchers.eq(false), any(), any());
    }
}
