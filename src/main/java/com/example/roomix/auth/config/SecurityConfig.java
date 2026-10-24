package com.example.roomix.auth.config;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * Seguridad de la API: autenticación stateless con JWT (HS256), contraseñas con BCrypt
 * y control de acceso por rol en cada módulo.
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private static final String ADMIN = Rol.ADMIN.name();
    private static final String RECEPCION = Rol.RECEPCION.name();
    private static final String HOUSEKEEPING = Rol.HOUSEKEEPING.name();
    private static final String FINANZAS = Rol.FINANZAS.name();
    private static final String EVENTOS = Rol.EVENTOS.name();
    private static final String ALMACEN = Rol.ALMACEN.name();

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtConverter
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> { })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/recuperar", "/api/auth/restablecer").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**").permitAll()
                        // Las fotos se cargan con <img>, que no envía el token
                        .requestMatchers(HttpMethod.GET, "/api/habitaciones/*/fotos/*").permitAll()

                        // Administración del sistema
                        .requestMatchers("/api/usuarios/**", "/api/auditoria/**").hasRole(ADMIN)

                        // Habitaciones
                        .requestMatchers(HttpMethod.GET, "/api/habitaciones/**")
                        .hasAnyRole(ADMIN, RECEPCION, HOUSEKEEPING, EVENTOS)
                        .requestMatchers(HttpMethod.POST, "/api/habitaciones").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/habitaciones/**").hasRole(ADMIN)
                        .requestMatchers("/api/habitaciones/*/fotos/**").hasRole(ADMIN)
                        .requestMatchers("/api/habitaciones/**").hasAnyRole(ADMIN, RECEPCION)

                        // Reservas, huéspedes y tarifas
                        .requestMatchers(HttpMethod.GET, "/api/reservas/**", "/api/huespedes/**")
                        .hasAnyRole(ADMIN, RECEPCION, FINANZAS)
                        .requestMatchers("/api/reservas/**", "/api/huespedes/**").hasAnyRole(ADMIN, RECEPCION)
                        .requestMatchers(HttpMethod.GET, "/api/tarifas/**").authenticated()
                        .requestMatchers("/api/tarifas/**").hasAnyRole(ADMIN, FINANZAS)

                        // Incidencias (limpieza, mantenimiento) y personal
                        .requestMatchers("/api/incidencias/**").hasAnyRole(ADMIN, RECEPCION, HOUSEKEEPING)
                        .requestMatchers(HttpMethod.GET, "/api/personal/**")
                        .hasAnyRole(ADMIN, RECEPCION, HOUSEKEEPING, EVENTOS)
                        .requestMatchers("/api/personal/**").hasRole(ADMIN)

                        // Inventario
                        .requestMatchers(HttpMethod.GET, "/api/inventario/**")
                        .hasAnyRole(ADMIN, ALMACEN, HOUSEKEEPING, FINANZAS)
                        .requestMatchers("/api/inventario/**").hasAnyRole(ADMIN, ALMACEN)

                        // Servicios
                        .requestMatchers(HttpMethod.GET, "/api/servicios/**")
                        .hasAnyRole(ADMIN, RECEPCION, HOUSEKEEPING, EVENTOS, FINANZAS)
                        .requestMatchers("/api/servicios/catalogo/**").hasAnyRole(ADMIN, EVENTOS)
                        .requestMatchers("/api/servicios/**").hasAnyRole(ADMIN, RECEPCION, HOUSEKEEPING, EVENTOS)

                        // Eventos y salones
                        .requestMatchers(HttpMethod.GET, "/api/eventos/**", "/api/salones/**")
                        .hasAnyRole(ADMIN, EVENTOS, RECEPCION, FINANZAS)
                        .requestMatchers("/api/eventos/**", "/api/salones/**").hasAnyRole(ADMIN, EVENTOS)

                        // Finanzas, facturación y reportes
                        .requestMatchers("/api/facturas/**").hasAnyRole(ADMIN, FINANZAS, RECEPCION)
                        .requestMatchers("/api/finanzas/**", "/api/reportes/**").hasAnyRole(ADMIN, FINANZAS)

                        // Dashboard, perfil propio y cualquier otra ruta: usuario autenticado
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(entryPoint())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                );
        return http.build();
    }

    /**
     * Toma el rol y el estado del usuario desde la base de datos en cada petición: si se desactiva
     * un usuario o se le cambia el rol, el efecto es inmediato aunque su token siga vigente.
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtConverter(UsuarioRepository usuarioRepository) {
        return jwt -> {
            Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(jwt.getSubject())
                    .filter(Usuario::isActivo)
                    .orElseThrow(() -> new DisabledException("Usuario inexistente o desactivado"));
            return new JwtAuthenticationToken(
                    jwt,
                    List.of(new SimpleGrantedAuthority(usuario.getRol().autoridad())),
                    usuario.getUsername()
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.security.jwt-secret:}") String secreto) {
        byte[] material;
        if (StringUtils.hasText(secreto)) {
            material = secreto.getBytes(StandardCharsets.UTF_8);
        } else {
            material = new byte[64];
            new SecureRandom().nextBytes(material);
            log.warn("JWT_SECRET no está definido: se generó una clave aleatoria. "
                    + "Las sesiones se cerrarán cada vez que el backend se reinicie. "
                    + "Defina JWT_SECRET en producción.");
        }
        return new SecretKeySpec(sha256(material), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> escribirError(
                response, HttpStatus.UNAUTHORIZED, "No autenticado",
                "Inicie sesión para acceder a este recurso", "AUT-401");
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> escribirError(
                response, HttpStatus.FORBIDDEN, "Acceso denegado",
                "Su rol no tiene permiso para realizar esta acción", "AUT-403");
    }

    private static void escribirError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String titulo,
            String detalle,
            String codigo
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","codigo":"%s","modulo":"AUTENTICACION"}"""
                .formatted(titulo, status.value(), detalle, codigo));
    }

    private static byte[] sha256(byte[] material) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(material);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
