package com.example.roomix.finanzas.repository;

import com.example.roomix.finanzas.domain.FacturaItem;
import com.example.roomix.finanzas.domain.OrigenItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaItemRepository extends JpaRepository<FacturaItem, Long> {

    /** ¿El registro origen ya está incluido en una factura vigente (no anulada)? */
    @Query("""
            SELECT COUNT(i) > 0 FROM FacturaItem i
            WHERE i.origen = :origen AND i.referenciaId = :referenciaId
              AND i.factura.estado <> com.example.roomix.finanzas.domain.EstadoFactura.ANULADA
            """)
    boolean estaFacturado(@Param("origen") OrigenItem origen, @Param("referenciaId") Long referenciaId);
}
