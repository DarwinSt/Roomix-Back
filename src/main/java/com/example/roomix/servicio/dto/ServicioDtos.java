package com.example.roomix.servicio.dto;

import com.example.roomix.servicio.domain.CategoriaServicio;
import com.example.roomix.servicio.domain.EstadoSolicitud;
import com.example.roomix.servicio.domain.ServicioCatalogo;
import com.example.roomix.servicio.domain.SolicitudServicio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ServicioDtos {

    private ServicioDtos() {
    }

    @Schema(name = "ServicioCatalogoRequest")
    public record CatalogoRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @Size(max = 400) String descripcion,
            @NotNull(message = "La categoría es obligatoria") CategoriaServicio categoria,
            @NotNull(message = "El precio es obligatorio")
            @DecimalMin(value = "0", message = "El precio no puede ser negativo")
            BigDecimal precio,
            @NotBlank(message = "La unidad de cobro es obligatoria") @Size(max = 40) String unidad,
            @Min(1) @Max(1440) Integer duracionMinutos,
            Boolean activo
    ) {
    }

    @Schema(name = "ServicioCatalogoResponse")
    public record CatalogoResponse(
            Long id,
            String nombre,
            String descripcion,
            CategoriaServicio categoria,
            String categoriaNombre,
            BigDecimal precio,
            String unidad,
            Integer duracionMinutos,
            boolean activo
    ) {
        public static CatalogoResponse from(ServicioCatalogo s) {
            return new CatalogoResponse(s.getId(), s.getNombre(), s.getDescripcion(), s.getCategoria(),
                    s.getCategoria().getNombre(), s.getPrecio(), s.getUnidad(), s.getDuracionMinutos(), s.isActivo());
        }
    }

    @Schema(name = "SolicitudServicioRequest")
    public record SolicitudRequest(
            @NotNull(message = "La reserva es obligatoria") Long reservaId,
            @NotNull(message = "El servicio es obligatorio") Long servicioId,
            @NotNull @Min(value = 1, message = "La cantidad mínima es 1") @Max(99) Integer cantidad,
            LocalDateTime fechaHoraProgramada,
            Long responsableId,
            @Size(max = 400) String notas
    ) {
    }

    @Schema(name = "CambiarEstadoSolicitudRequest")
    public record CambiarEstadoRequest(
            @NotNull EstadoSolicitud estado,
            @Size(max = 200) String motivo
    ) {
    }

    @Schema(name = "AsignarSolicitudRequest")
    public record AsignarRequest(
            Long responsableId,
            LocalDateTime fechaHoraProgramada
    ) {
    }

    @Schema(name = "SolicitudServicioResponse")
    public record SolicitudResponse(
            Long id,
            Long servicioId,
            String servicioNombre,
            CategoriaServicio categoria,
            String unidad,
            Long reservaId,
            Long habitacionId,
            String habitacionNumero,
            Long huespedId,
            String huespedNombre,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal total,
            LocalDateTime fechaHoraProgramada,
            Long responsableId,
            String responsableNombre,
            EstadoSolicitud estado,
            String notas,
            String motivoCancelacion,
            String solicitadoPor,
            LocalDateTime fechaHoraSolicitud,
            LocalDateTime fechaHoraCompletado
    ) {
        public static SolicitudResponse from(SolicitudServicio s) {
            return new SolicitudResponse(
                    s.getId(),
                    s.getServicio().getId(),
                    s.getServicio().getNombre(),
                    s.getServicio().getCategoria(),
                    s.getServicio().getUnidad(),
                    s.getReserva().getId(),
                    s.getHabitacion().getId(),
                    s.getHabitacion().getNumero(),
                    s.getHuesped().getId(),
                    s.getHuesped().nombreCompleto(),
                    s.getCantidad(),
                    s.getPrecioUnitario(),
                    s.getTotal(),
                    s.getFechaHoraProgramada(),
                    s.getResponsable() != null ? s.getResponsable().getId() : null,
                    s.getResponsable() != null ? s.getResponsable().getNombre() : null,
                    s.getEstado(),
                    s.getNotas(),
                    s.getMotivoCancelacion(),
                    s.getSolicitadoPor(),
                    s.getFechaHoraSolicitud(),
                    s.getFechaHoraCompletado()
            );
        }
    }
}
