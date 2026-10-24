package com.example.roomix.auditoria.service;

import com.example.roomix.auditoria.domain.RegistroAuditoria;
import com.example.roomix.auditoria.repository.RegistroAuditoriaRepository;
import com.example.roomix.common.dto.PaginaResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;

    /**
     * Guarda el registro en una transacción propia: la auditoría no debe perderse
     * si la operación de negocio hace rollback, ni romper la operación si falla.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(RegistroAuditoria registro) {
        try {
            if (registro.getFechaHora() == null) {
                registro.setFechaHora(LocalDateTime.now());
            }
            registro.setDetalle(recortar(registro.getDetalle(), 500));
            registro.setRuta(recortar(registro.getRuta(), 200));
            repository.save(registro);
        } catch (Exception ex) {
            log.warn("No se pudo guardar el registro de auditoría: {}", ex.getMessage());
        }
    }

    public void registrarEvento(
            String usuario,
            String rol,
            String accion,
            String modulo,
            boolean exitoso,
            String detalle,
            HttpServletRequest request
    ) {
        registrar(RegistroAuditoria.builder()
                .usuario(StringUtils.hasText(usuario) ? recortar(usuario, 50) : "anonimo")
                .rol(rol)
                .accion(accion)
                .modulo(modulo)
                .exitoso(exitoso)
                .detalle(detalle)
                .metodoHttp(request != null ? request.getMethod() : null)
                .ruta(request != null ? request.getRequestURI() : null)
                .ip(request != null ? ipCliente(request) : null)
                .build());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<RegistroAuditoriaResponse> buscar(
            String usuario,
            String modulo,
            String accion,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamano
    ) {
        LocalDateTime inicio = (desde != null ? desde : LocalDate.now().minusDays(30)).atStartOfDay();
        LocalDateTime fin = (hasta != null ? hasta : LocalDate.now()).plusDays(1).atStartOfDay();
        return PaginaResponse.de(
                repository.buscar(
                        vacioANull(usuario),
                        vacioANull(modulo),
                        vacioANull(accion),
                        inicio,
                        fin,
                        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 200))
                ),
                RegistroAuditoriaResponse::from
        );
    }

    @Transactional(readOnly = true)
    public List<String> modulos() {
        return repository.modulos();
    }

    /** IP real del cliente detrás del proxy de la plataforma (Render, Railway). */
    public static String ipCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return recortar(forwarded.split(",")[0].trim(), 64);
        }
        return recortar(request.getRemoteAddr(), 64);
    }

    private static String vacioANull(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private static String recortar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    public record RegistroAuditoriaResponse(
            Long id,
            LocalDateTime fechaHora,
            String usuario,
            String rol,
            String accion,
            String modulo,
            String entidadId,
            String metodoHttp,
            String ruta,
            Integer estadoHttp,
            boolean exitoso,
            String ip,
            String detalle
    ) {
        static RegistroAuditoriaResponse from(RegistroAuditoria a) {
            return new RegistroAuditoriaResponse(
                    a.getId(), a.getFechaHora(), a.getUsuario(), a.getRol(), a.getAccion(), a.getModulo(),
                    a.getEntidadId(), a.getMetodoHttp(), a.getRuta(), a.getEstadoHttp(), a.isExitoso(),
                    a.getIp(), a.getDetalle()
            );
        }
    }
}
