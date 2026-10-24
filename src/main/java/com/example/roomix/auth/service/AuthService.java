package com.example.roomix.auth.service;

import com.example.roomix.auditoria.service.AuditoriaService;
import com.example.roomix.auth.domain.TokenRecuperacion;
import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.dto.AuthDtos.CambiarPasswordRequest;
import com.example.roomix.auth.dto.AuthDtos.LoginRequest;
import com.example.roomix.auth.dto.AuthDtos.LoginResponse;
import com.example.roomix.auth.dto.AuthDtos.RestablecerPasswordRequest;
import com.example.roomix.auth.dto.AuthDtos.UsuarioResponse;
import com.example.roomix.auth.exception.AuthErrorCode;
import com.example.roomix.auth.exception.AuthException;
import com.example.roomix.auth.repository.TokenRecuperacionRepository;
import com.example.roomix.auth.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Inicio de sesión, cambio de contraseña y recuperación de credenciales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    static final int MAX_INTENTOS = 5;
    static final int MINUTOS_BLOQUEO = 15;
    static final int MINUTOS_VIGENCIA_TOKEN = 30;
    private static final String MODULO = "AUTENTICACION";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final CorreoService correoService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private volatile String hashFicticio;

    private String hashFicticio() {
        if (hashFicticio == null) {
            hashFicticio = passwordEncoder.encode("roomix-usuario-inexistente");
        }
        return hashFicticio;
    }

    /**
     * Valida credenciales y emite el token. Tras {@value #MAX_INTENTOS} intentos fallidos
     * consecutivos la cuenta queda bloqueada {@value #MINUTOS_BLOQUEO} minutos.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public LoginResponse login(LoginRequest request, HttpServletRequest http) {
        LocalDateTime ahora = LocalDateTime.now();
        String username = request.username().trim();
        Optional<Usuario> encontrado = usuarioRepository.findByUsernameIgnoreCase(username);

        if (encontrado.isEmpty()) {
            // Mismo costo que una verificación real para no revelar qué usuarios existen
            passwordEncoder.matches(request.password(), hashFicticio());
            auditoriaService.registrarEvento(username, null, "LOGIN_FALLIDO", MODULO, false, "Usuario inexistente", http);
            throw new AuthException(AuthErrorCode.CREDENCIALES_INVALIDAS);
        }

        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado(ahora)) {
            auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "LOGIN_FALLIDO", MODULO,
                    false, "Cuenta bloqueada", http);
            throw new AuthException(AuthErrorCode.USUARIO_BLOQUEADO,
                    usuario.getBloqueadoHasta().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);
            String detalle = "Contraseña incorrecta (intento " + intentos + " de " + MAX_INTENTOS + ")";
            if (intentos >= MAX_INTENTOS) {
                usuario.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                usuario.setIntentosFallidos(0);
                detalle = "Cuenta bloqueada " + MINUTOS_BLOQUEO + " minutos por intentos fallidos";
            }
            usuarioRepository.save(usuario);
            auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "LOGIN_FALLIDO", MODULO,
                    false, detalle, http);
            throw new AuthException(AuthErrorCode.CREDENCIALES_INVALIDAS);
        }

        if (!usuario.isActivo()) {
            auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "LOGIN_FALLIDO", MODULO,
                    false, "Usuario desactivado", http);
            throw new AuthException(AuthErrorCode.USUARIO_INACTIVO);
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimoAcceso(ahora);
        usuarioRepository.save(usuario);

        JwtService.TokenEmitido token = jwtService.emitir(usuario);
        auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "LOGIN", MODULO, true,
                "Inicio de sesión", http);
        return new LoginResponse(token.token(), "Bearer", token.expiraEn(), UsuarioResponse.from(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil() {
        return UsuarioResponse.from(usuarioAutenticado());
    }

    @Transactional
    public void cambiarPassword(CambiarPasswordRequest request, HttpServletRequest http) {
        Usuario usuario = usuarioAutenticado();
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "CAMBIO_PASSWORD", MODULO,
                    false, "Contraseña actual incorrecta", http);
            throw new AuthException(AuthErrorCode.PASSWORD_ACTUAL_INCORRECTA);
        }
        if (request.passwordActual().equals(request.passwordNueva())) {
            throw new AuthException(AuthErrorCode.PASSWORD_REPETIDA);
        }
        PasswordPolicy.validar(request.passwordNueva());
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "CAMBIO_PASSWORD", MODULO,
                true, "Contraseña actualizada", http);
    }

    /**
     * Genera un enlace de recuperación de un solo uso (válido {@value #MINUTOS_VIGENCIA_TOKEN} minutos)
     * y lo envía por correo. Siempre responde igual, exista o no el correo, para no revelar usuarios.
     *
     * @return true si el correo se envió (o no aplica); false si el SMTP no está configurado
     */
    @Transactional
    public boolean solicitarRecuperacion(String email, HttpServletRequest http) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(Usuario::isActivo);
        if (encontrado.isEmpty()) {
            auditoriaService.registrarEvento(email, null, "RECUPERAR_PASSWORD", MODULO, false,
                    "Correo no registrado o usuario inactivo", http);
            return correoService.habilitado();
        }

        Usuario usuario = encontrado.get();
        tokenRepository.invalidarPendientes(usuario.getId());

        byte[] aleatorio = new byte[32];
        new SecureRandom().nextBytes(aleatorio);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        LocalDateTime ahora = LocalDateTime.now();
        tokenRepository.save(TokenRecuperacion.builder()
                .usuario(usuario)
                .tokenHash(sha256(token))
                .expiraEn(ahora.plusMinutes(MINUTOS_VIGENCIA_TOKEN))
                .fechaHoraCreacion(ahora)
                .build());

        String enlace = frontendUrl.replaceAll("/+$", "") + "/restablecer?token=" + token;
        boolean enviado = correoService.enviar(
                usuario.getEmail(),
                "Roomix — Restablecer contraseña",
                """
                Hola %s,

                Recibimos una solicitud para restablecer tu contraseña de Roomix.
                Abre este enlace (válido por %d minutos):

                %s

                Si no fuiste tú, ignora este mensaje: tu contraseña no cambiará.
                """.formatted(usuario.getNombreCompleto(), MINUTOS_VIGENCIA_TOKEN, enlace)
        );
        if (!enviado) {
            log.info("Enlace de recuperación para {} (SMTP no configurado): {}", usuario.getUsername(), enlace);
        }
        auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "RECUPERAR_PASSWORD", MODULO,
                true, enviado ? "Enlace enviado por correo" : "Enlace generado (correo no configurado)", http);
        return enviado;
    }

    @Transactional
    public void restablecer(RestablecerPasswordRequest request, HttpServletRequest http) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(sha256(request.token().trim()))
                .filter(t -> !t.isUsado() && t.getExpiraEn().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new AuthException(AuthErrorCode.TOKEN_INVALIDO));

        PasswordPolicy.validar(request.passwordNueva());
        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setDebeCambiarPassword(false);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);
        auditoriaService.registrarEvento(usuario.getUsername(), usuario.getRol().name(), "RESTABLECER_PASSWORD",
                MODULO, true, "Contraseña restablecida con enlace de recuperación", http);
    }

    private Usuario usuarioAutenticado() {
        return usuarioRepository.findByUsernameIgnoreCase(UsuarioActual.username())
                .orElseThrow(() -> new AuthException(AuthErrorCode.CREDENCIALES_INVALIDAS));
    }

    static String sha256(String valor) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
