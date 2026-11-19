package com.example.roomix.auth.service;

import com.example.roomix.auth.domain.Permiso;
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
    private final com.example.roomix.config.Licencia licencia;
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
        aplicarPermisos(usuario, request);
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
        validarNoQuitaUltimoAdmin(usuario, permisosSolicitados(request).contains(Permiso.USUARIOS), seraActivo);

        usuario.setUsername(username);
        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setEmail(email);
        usuario.setRol(request.rol());
        aplicarPermisos(usuario, request);
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
        validarNoQuitaUltimoAdmin(usuario, usuario.tienePermiso(Permiso.USUARIOS), activo);
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

    /** Permisos que pide la solicitud: los marcados o, si no se envían, los del rol. */
    private static java.util.Set<Permiso> permisosSolicitados(UsuarioRequest request) {
        if (request.permisos() == null) {
            return Permiso.porDefecto(request.rol());
        }
        java.util.EnumSet<Permiso> set = java.util.EnumSet.noneOf(Permiso.class);
        request.permisos().stream().filter(java.util.Objects::nonNull).forEach(set::add);
        return set;
    }

    /** Si los permisos marcados son los del rol, el usuario sigue la plantilla (y recibe los cambios futuros del rol). */
    private static void aplicarPermisos(Usuario usuario, UsuarioRequest request) {
        java.util.Set<Permiso> pedidos = permisosSolicitados(request);
        boolean personalizados = !pedidos.equals(Permiso.porDefecto(request.rol()));
        usuario.getPermisos().clear();
        if (personalizados) {
            usuario.getPermisos().addAll(pedidos);
        }
        usuario.setPermisosPersonalizados(personalizados);
    }

    /** No se puede dejar el sistema sin ningún usuario activo que administre los usuarios. */
    private void validarNoQuitaUltimoAdmin(Usuario usuario, boolean seguiraSiendoAdmin, boolean seraActivo) {
        boolean esAdminActivo = usuario.tienePermiso(Permiso.USUARIOS) && usuario.isActivo();
        boolean dejaDeSerlo = !seguiraSiendoAdmin || !seraActivo;
        long administradores = usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo() && u.tienePermiso(Permiso.USUARIOS)).count();
        if (esAdminActivo && dejaDeSerlo && administradores <= 1) {
            throw new AuthException(AuthErrorCode.ULTIMO_ADMIN);
        }
    }

    public com.example.roomix.auth.dto.AuthDtos.CatalogoPermisos catalogoPermisos() {
        var permisos = java.util.Arrays.stream(Permiso.values())
                .map(p -> new com.example.roomix.auth.dto.AuthDtos.PermisoInfo(p, p.getGrupo(), p.getNivel(), p.getDescripcion(),
                        p.getModulo() == null || licencia.habilitado(p.getModulo())))
                .toList();
        java.util.Map<Rol, java.util.List<Permiso>> porRol = new java.util.EnumMap<>(Rol.class);
        for (Rol r : Rol.values()) {
            porRol.put(r, java.util.List.copyOf(Permiso.porDefecto(r)));
        }
        return new com.example.roomix.auth.dto.AuthDtos.CatalogoPermisos(permisos, porRol);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USUARIO_NO_ENCONTRADO, id));
    }

    private static String normalizarUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
