package com.example.roomix.auth.service;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.dto.AuthDtos.PasswordTemporalResponse;
import com.example.roomix.auth.dto.AuthDtos.UsuarioRequest;
import com.example.roomix.auth.dto.AuthDtos.UsuarioResponse;
import com.example.roomix.auth.exception.AuthErrorCode;
import com.example.roomix.auth.exception.AuthException;
import com.example.roomix.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Administración de cuentas de usuario (rol ADMIN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByNombreCompletoAsc().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String username = normalizarUsername(request.username());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new AuthException(AuthErrorCode.USERNAME_DUPLICADO, username);
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_DUPLICADO, email);
        }
        PasswordPolicy.validar(request.password());

        Usuario usuario = Usuario.builder()
                .username(username)
                .nombreCompleto(request.nombreCompleto().trim())
                .email(email)
                .rol(request.rol())
                .activo(request.activo() == null || request.activo())
                .passwordHash(passwordEncoder.encode(request.password()))
                // La contraseña la definió el administrador: el usuario debe cambiarla al ingresar
                .debeCambiarPassword(true)
                .build();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscar(id);
        String username = normalizarUsername(request.username());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new AuthException(AuthErrorCode.USERNAME_DUPLICADO, username);
        }
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new AuthException(AuthErrorCode.EMAIL_DUPLICADO, email);
        }

        boolean seraActivo = request.activo() == null ? usuario.isActivo() : request.activo();
        validarNoQuitaUltimoAdmin(usuario, request.rol(), seraActivo);

        usuario.setUsername(username);
        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setEmail(email);
        usuario.setRol(request.rol());
        usuario.setActivo(seraActivo);
        if (StringUtils.hasText(request.password())) {
            PasswordPolicy.validar(request.password());
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
            usuario.setDebeCambiarPassword(true);
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse cambiarActivo(Long id, boolean activo) {
        Usuario usuario = buscar(id);
        validarNoQuitaUltimoAdmin(usuario, usuario.getRol(), activo);
        usuario.setActivo(activo);
        if (activo) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /** Genera una contraseña temporal (se muestra una sola vez) y desbloquea la cuenta. */
    @Transactional
    public PasswordTemporalResponse restablecerPassword(Long id) {
        Usuario usuario = buscar(id);
        String temporal = PasswordPolicy.generarTemporal();
        usuario.setPasswordHash(passwordEncoder.encode(temporal));
        usuario.setDebeCambiarPassword(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        return new PasswordTemporalResponse(temporal);
    }

    private void validarNoQuitaUltimoAdmin(Usuario usuario, Rol nuevoRol, boolean seraActivo) {
        boolean esAdminActivo = usuario.getRol() == Rol.ADMIN && usuario.isActivo();
        boolean dejaDeSerlo = nuevoRol != Rol.ADMIN || !seraActivo;
        if (esAdminActivo && dejaDeSerlo && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            throw new AuthException(AuthErrorCode.ULTIMO_ADMIN);
        }
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USUARIO_NO_ENCONTRADO, id));
    }

    private static String normalizarUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
