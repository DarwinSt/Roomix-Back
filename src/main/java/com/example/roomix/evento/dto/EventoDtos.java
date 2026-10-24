package com.example.roomix.evento.dto;

import com.example.roomix.evento.domain.Evento;
import com.example.roomix.evento.domain.EventoEnums.EstadoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoAcceso;
import com.example.roomix.evento.domain.EventoEnums.TipoEvento;
import com.example.roomix.evento.domain.EventoEnums.TipoRecurso;
import com.example.roomix.evento.domain.EventoParticipante;
import com.example.roomix.evento.domain.EventoRecurso;
import com.example.roomix.evento.domain.Salon;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class EventoDtos {

    private EventoDtos() {
    }

    @Schema(name = "SalonRequest")
    public record SalonRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @NotNull(message = "La capacidad es obligatoria") @Min(1) @Max(5000) Integer capacidad,
            @Size(max = 300) String descripcion,
            @NotNull(message = "La tarifa por hora es obligatoria") @DecimalMin("0") BigDecimal tarifaHora,
            Boolean activo
    ) {
    }

    @Schema(name = "SalonResponse")
    public record SalonResponse(Long id, String nombre, Integer capacidad, String descripcion, BigDecimal tarifaHora, boolean activo) {
        public static SalonResponse from(Salon s) {
            return new SalonResponse(s.getId(), s.getNombre(), s.getCapacidad(), s.getDescripcion(), s.getTarifaHora(), s.isActivo());
        }
    }

    @Schema(name = "RecursoEventoRequest")
    public record RecursoRequest(
            @NotNull TipoRecurso tipo,
            @NotBlank @Size(max = 150) String descripcion,
            @NotNull @Min(1) Integer cantidad,
            @NotNull @DecimalMin("0") BigDecimal valorUnitario
    ) {
    }

    @Schema(name = "EventoRequest")
    public record EventoRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
            @Size(max = 600) String descripcion,
            @NotNull(message = "El tipo de evento es obligatorio") TipoEvento tipo,
            @NotNull(message = "El tipo de acceso es obligatorio") TipoAcceso tipoAcceso,
            @NotNull(message = "La fecha y hora de inicio es obligatoria") LocalDateTime fechaHoraInicio,
            @NotNull(message = "La fecha y hora de fin es obligatoria") LocalDateTime fechaHoraFin,
            @NotNull(message = "El salón es obligatorio") Long salonId,
            Long responsableId,
            @NotNull(message = "La capacidad es obligatoria") @Min(1) Integer capacidad,
            @NotBlank(message = "El organizador es obligatorio") @Size(max = 150) String organizadorNombre,
            @Size(max = 40) String organizadorDocumento,
            @Size(max = 30) String organizadorTelefono,
            @Email @Size(max = 120) String organizadorEmail,
            @NotNull(message = "El valor base es obligatorio") @DecimalMin("0") BigDecimal valorBase,
            @Size(max = 500) String notas,
            List<@Valid RecursoRequest> recursos
    ) {
    }

    @Schema(name = "ParticipanteRequest")
    public record ParticipanteRequest(
            @Schema(description = "Si se indica, se toman los datos del huésped") Long huespedId,
            @Size(max = 150) String nombre,
            @Size(max = 40) String documento,
            @Email @Size(max = 120) String email,
            @Size(max = 30) String telefono,
            Boolean confirmado
    ) {
    }

    @Schema(name = "CambiarEstadoEventoRequest")
    public record CambiarEstadoRequest(@NotNull EstadoEvento estado, @Size(max = 200) String motivo) {
    }

    @Schema(name = "RecursoEventoResponse")
    public record RecursoResponse(Long id, TipoRecurso tipo, String tipoNombre, String descripcion, Integer cantidad,
                                  BigDecimal valorUnitario, BigDecimal total) {
        public static RecursoResponse from(EventoRecurso r) {
            return new RecursoResponse(r.getId(), r.getTipo(), r.getTipo().getNombre(), r.getDescripcion(),
                    r.getCantidad(), r.getValorUnitario(), r.getTotal());
        }
    }

    @Schema(name = "ParticipanteResponse")
    public record ParticipanteResponse(Long id, Long huespedId, String nombre, String documento, String email,
                                       String telefono, boolean confirmado, boolean asistio, LocalDateTime fechaHoraRegistro) {
        public static ParticipanteResponse from(EventoParticipante p) {
            return new ParticipanteResponse(p.getId(), p.getHuesped() != null ? p.getHuesped().getId() : null,
                    p.getNombre(), p.getDocumento(), p.getEmail(), p.getTelefono(), p.isConfirmado(), p.isAsistio(),
                    p.getFechaHoraRegistro());
        }
    }

    @Schema(name = "EventoResponse")
    public record EventoResponse(
            Long id,
            String nombre,
            String descripcion,
            TipoEvento tipo,
            String tipoNombre,
            TipoAcceso tipoAcceso,
            String tipoAccesoNombre,
            EstadoEvento estado,
            LocalDateTime fechaHoraInicio,
            LocalDateTime fechaHoraFin,
            Long salonId,
            String salonNombre,
            Integer salonCapacidad,
            Long responsableId,
            String responsableNombre,
            Integer capacidad,
            int inscritos,
            long confirmados,
            long asistentes,
            String organizadorNombre,
            String organizadorDocumento,
            String organizadorTelefono,
            String organizadorEmail,
            BigDecimal valorBase,
            BigDecimal totalRecursos,
            BigDecimal valorTotal,
            Long facturaId,
            String facturaNumero,
            EstadoFactura facturaEstado,
            String notas,
            String motivoCancelacion,
            String creadoPor,
            List<RecursoResponse> recursos,
            List<ParticipanteResponse> participantes
    ) {
        public static EventoResponse from(Evento e, Factura factura) {
            return new EventoResponse(
                    e.getId(), e.getNombre(), e.getDescripcion(), e.getTipo(), e.getTipo().getNombre(),
                    e.getTipoAcceso(), e.getTipoAcceso().getNombre(), e.getEstado(),
                    e.getFechaHoraInicio(), e.getFechaHoraFin(),
                    e.getSalon().getId(), e.getSalon().getNombre(), e.getSalon().getCapacidad(),
                    e.getResponsable() != null ? e.getResponsable().getId() : null,
                    e.getResponsable() != null ? e.getResponsable().getNombre() : null,
                    e.getCapacidad(),
                    e.getParticipantes().size(),
                    e.getParticipantes().stream().filter(EventoParticipante::isConfirmado).count(),
                    e.getParticipantes().stream().filter(EventoParticipante::isAsistio).count(),
                    e.getOrganizadorNombre(), e.getOrganizadorDocumento(), e.getOrganizadorTelefono(), e.getOrganizadorEmail(),
                    e.getValorBase(), e.totalRecursos(), e.valorTotal(),
                    factura != null ? factura.getId() : null,
                    factura != null ? factura.getNumero() : null,
                    factura != null ? factura.getEstado() : null,
                    e.getNotas(), e.getMotivoCancelacion(), e.getCreadoPor(),
                    e.getRecursos().stream().map(RecursoResponse::from).toList(),
                    e.getParticipantes().stream().map(ParticipanteResponse::from).toList()
            );
        }
    }

    @Schema(name = "ReporteSalon")
    public record ReporteSalon(String salon, long eventos, long participantes, BigDecimal valor) {
    }

    @Schema(name = "ReporteEventosResponse")
    public record ReporteEventos(
            LocalDate desde,
            LocalDate hasta,
            long totalEventos,
            Map<EstadoEvento, Long> porEstado,
            long totalInscritos,
            long totalAsistentes,
            @Schema(description = "Valor de eventos no cancelados") BigDecimal valorTotal,
            @Schema(description = "Total de facturas de eventos (no anuladas)") BigDecimal facturado,
            @Schema(description = "Pagos recibidos por eventos") BigDecimal recaudado,
            List<ReporteSalon> porSalon
    ) {
    }
}
