package com.example.roomix.auth.config;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Crea las cuentas iniciales cuando la tabla de usuarios está vacía:
 * el administrador (ADMIN_USERNAME / ADMIN_PASSWORD) y, si APP_DEMO_USERS=true,
 * un usuario de demostración por cada rol (DEMO_USERS_PASSWORD).
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class UsuarioSeed implements ApplicationRunner {

    static final String PASSWORD_POR_DEFECTO = "Roomix2026*";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.usuarios-demo:true}")
    private boolean usuariosDemo;

    @Value("${app.seed.demo-password:}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String passwordAdmin = StringUtils.hasText(adminPassword) ? adminPassword : PASSWORD_POR_DEFECTO;
        if (!StringUtils.hasText(adminPassword)) {
            log.warn("ADMIN_PASSWORD no está definido: el administrador '{}' usa la contraseña por defecto. "
                    + "Cámbiela tras el primer ingreso.", adminUsername);
        }
        crear(adminUsername, "Administrador Roomix", "admin@roomix.test", Rol.ADMIN, passwordAdmin);

        if (usuariosDemo) {
            String passwordDemo = StringUtils.hasText(demoPassword) ? demoPassword : PASSWORD_POR_DEFECTO;
            crear("recepcion", "Carmen Rosa Villadiego", "recepcion@roomix.test", Rol.RECEPCION, passwordDemo);
            crear("housekeeping", "Luz Marina Cárdenas", "housekeeping@roomix.test", Rol.HOUSEKEEPING, passwordDemo);
            crear("finanzas", "Patricia Gómez Salcedo", "finanzas@roomix.test", Rol.FINANZAS, passwordDemo);
            crear("eventos", "Mauricio Ruiz Torres", "eventos@roomix.test", Rol.EVENTOS, passwordDemo);
            crear("almacen", "Óscar Castillo Mendoza", "almacen@roomix.test", Rol.ALMACEN, passwordDemo);
            log.info("Usuarios de demostración creados (uno por rol)");
        }
    }

    private void crear(String username, String nombre, String email, Rol rol, String password) {
        usuarioRepository.save(Usuario.builder()
                .username(username.toLowerCase())
                .nombreCompleto(nombre)
                .email(email)
                .rol(rol)
                .passwordHash(passwordEncoder.encode(password))
                .activo(true)
                .build());
    }
}
