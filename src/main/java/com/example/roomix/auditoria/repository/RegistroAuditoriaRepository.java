package com.example.roomix.auditoria.repository;

import com.example.roomix.auditoria.domain.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    @Query("""
            SELECT a FROM RegistroAuditoria a
            WHERE (CAST(:usuario AS String) IS NULL OR LOWER(a.usuario) = LOWER(CAST(:usuario AS String)))
              AND (CAST(:modulo AS String) IS NULL OR a.modulo = CAST(:modulo AS String))
              AND (CAST(:accion AS String) IS NULL OR a.accion = CAST(:accion AS String))
              AND a.fechaHora >= :desde
              AND a.fechaHora < :hasta
            ORDER BY a.fechaHora DESC, a.id DESC
            """)
    Page<RegistroAuditoria> buscar(
            @Param("usuario") String usuario,
            @Param("modulo") String modulo,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

    @Query("SELECT DISTINCT a.modulo FROM RegistroAuditoria a ORDER BY a.modulo")
    List<String> modulos();
}
