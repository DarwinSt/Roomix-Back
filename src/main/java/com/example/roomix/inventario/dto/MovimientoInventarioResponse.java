package com.example.roomix.inventario.dto;

import com.example.roomix.inventario.domain.MovimientoInventario;
import com.example.roomix.inventario.domain.TipoMovimientoStock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "MovimientoInventarioResponse")
public record MovimientoInventarioResponse(
        Long id,
        Long articuloId,
        String articuloNombre,
        TipoMovimientoStock tipo,
        Integer cantidad,
        Integer stockAnterior,
        Integer stockResultante,
        BigDecimal costoUnitario,
        String motivo,
        String proveedor,
        String registradoPor,
        LocalDateTime fechaHora
) {
    public static MovimientoInventarioResponse from(MovimientoInventario m) {
        return new MovimientoInventarioResponse(
                m.getId(), m.getArticulo().getId(), m.getArticulo().getNombre(), m.getTipo(), m.getCantidad(),
                m.getStockAnterior(), m.getStockResultante(), m.getCostoUnitario(), m.getMotivo(), m.getProveedor(),
                m.getRegistradoPor(), m.getFechaHora()
        );
    }
}
