package com.example.roomix.auth.repository;

import com.example.roomix.auth.domain.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE TokenRecuperacion t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    void invalidarPendientes(@Param("usuarioId") Long usuarioId);
}
