package com.example.roomix.auth.repository;

import com.example.roomix.auth.domain.Rol;
import com.example.roomix.auth.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByRolAndActivoTrue(Rol rol);

    /** Usuarios activos con el rol como principal o adicional. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(DISTINCT u) FROM Usuario u LEFT JOIN u.rolesAdicionales r
            WHERE u.activo = true AND (u.rol = :rol OR r = :rol)
            """)
    long contarActivosConRol(@org.springframework.data.repository.query.Param("rol") Rol rol);

    List<Usuario> findAllByOrderByNombreCompletoAsc();
}
