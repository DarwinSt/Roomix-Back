package com.example.roomix.venta.dto;

import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.venta.domain.EstadoVenta;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.domain.VentaProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class VentaDtos {

    private VentaDtos() {
    }

    @Schema(description = "Producto que se puede vender: tiene precio de venta y está activo")
    public record ProductoVenta(
            Long id,
            String nombre,
            String categoria,
            BigDecimal precioVenta,
            Integer disponible,
            String unidad
    ) {
    }

    @Schema(description = "Estadía a la que se pueden cargar consumos (huésped alojado)")
    public record EstadiaActiva(Long reservaId, String habitacion, String huesped, LocalDate fechaEntrada, LocalDate fechaSalida) {
    }

    public record ItemVentaRequest(
            @NotNull(message = "Indique el producto") Long articuloId,
            @NotNull(message = "Indique la cantidad")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            @Max(value = 999, message = "Cantidad no válida") Integer cantidad
    ) {
    }

    @Schema(description = "Venta de productos: de contado (entra a la caja) o cargada a la habitación (se paga en el check-out)")
    public record VentaRequest(
            @NotNull(message = "Indique cómo se paga la venta") ModoVenta modo,
            @Schema(description = "Reserva en curso (obligatoria si se carga a la habitación)") Long reservaId,
            @NotEmpty(message = "Agregue al menos un producto")
            @Size(max = 50, message = "Máximo 50 productos por venta") List<@Valid ItemVentaRequest> items,
            @Schema(description = "Medio de pago (obligatorio en ventas de contado)") MetodoPago metodoPago,
            @Size(max = 150) String cliente,
            @Size(max = 60) String referencia
    ) {
    }

    public record AnularVentaRequest(@NotBlank(message = "Indique el motivo") @Size(max = 300) String motivo) {
    }

    public record ItemVenta(Long id, Long articuloId, String nombre, Integer cantidad, BigDecimal precioUnitario, BigDecimal total) {
    }

    public record VentaResponse(
            Long id,
            ModoVenta modo,
            EstadoVenta estado,
            Long reservaId,
            String habitacion,
            String cliente,
            MetodoPago metodoPago,
            String metodoPagoNombre,
            String referencia,
            BigDecimal total,
            List<ItemVenta> items,
            String registradaPor,
            LocalDateTime fechaHora,
            String motivoAnulacion
    ) {
        public static VentaResponse from(VentaProducto v) {
            var reserva = v.getReserva();
            return new VentaResponse(v.getId(), v.getModo(), v.getEstado(),
                    reserva != null ? reserva.getId() : null,
                    reserva != null ? reserva.getHabitacion().getNumero() : null,
                    v.getCliente() != null ? v.getCliente() : reserva != null ? reserva.getHuesped().nombreCompleto() : null,
                    v.getMetodoPago(), v.getMetodoPago() != null ? v.getMetodoPago().getNombre() : null,
                    v.getReferencia(), v.getTotal(),
                    v.getItems().stream().map(i -> new ItemVenta(i.getId(), i.getArticuloId(), i.getNombre(), i.getCantidad(),
                            i.getPrecioUnitario(), i.getTotal())).toList(),
                    v.getRegistradaPor(), v.getFechaHora(), v.getMotivoAnulacion());
        }
    }
}
