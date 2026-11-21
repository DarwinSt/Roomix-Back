package com.example.roomix.venta.repository;

import com.example.roomix.venta.domain.EstadoVenta;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.domain.VentaProducto;
import com.example.roomix.venta.domain.VentaProductoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    List<VentaProducto> findByReservaIdOrderByFechaHoraDesc(Long reservaId);

    List<VentaProducto> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);

    /** Productos cargados a la cuenta de una estadía (para el estado de cuenta y la factura). */
    @Query("""
            select i from VentaProductoItem i join i.venta v
            where v.reserva.id = :reservaId and v.modo = :modo and v.estado = :estado
            order by v.fechaHora, i.id
            """)
    List<VentaProductoItem> itemsCargados(Long reservaId, ModoVenta modo, EstadoVenta estado);
}
