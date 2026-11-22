package com.example.roomix.auditoria.service;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.Factura;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import com.example.roomix.venta.domain.EstadoVenta;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.domain.VentaProducto;
import com.example.roomix.venta.domain.VentaProductoItem;
import com.example.roomix.venta.repository.VentaProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Reporte de anulaciones: une facturas, ventas y movimientos sin contar dos veces y marca a quien anuló lo propio. */
@ExtendWith(MockitoExtension.class)
class AnulacionesServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private MovimientoFinancieroRepository movimientoRepository;
    @Mock
    private VentaProductoRepository ventaRepository;
    @InjectMocks
    private AnulacionesService service;

    private static final LocalDateTime AYER = LocalDateTime.now().minusDays(1);

    private static MovimientoFinanciero movimiento(long id, Long facturaId, String referencia, LocalDateTime cuando) {
        return MovimientoFinanciero.builder().id(id).tipo(TipoMovimiento.EGRESO).categoria(CategoriaFinanciera.MANTENIMIENTO)
                .concepto("Bombillos").valor(new BigDecimal("20000")).facturaId(facturaId).referencia(referencia)
                .registradoPor("finanzas").anuladoPor("admin").fechaHoraAnulacion(cuando).anulado(true).build();
    }

    @Test
    void uneLasAnulacionesSinRepetirLasQueVienenDeUnaFacturaOUnaVenta() {
        when(facturaRepository.findByEstadoAndFechaHoraAnulacionBetween(eq(EstadoFactura.ANULADA), any(), any())).thenReturn(List.of(
                Factura.builder().id(1L).numero("FV-000001").clienteNombre("Ana").total(new BigDecimal("300000"))
                        .motivoAnulacion("Error en el valor").emitidaPor("recepcion").anuladaPor("admin").fechaHoraAnulacion(AYER).build()));
        VentaProducto venta = VentaProducto.builder().id(2L).modo(ModoVenta.CONTADO).estado(EstadoVenta.ANULADA)
                .total(new BigDecimal("4500")).registradaPor("admin").anuladaPor("admin").motivoAnulacion("Devolución")
                .fechaHoraAnulacion(AYER.plusHours(2)).build();
        venta.getItems().add(VentaProductoItem.builder().nombre("Gaseosa").cantidad(1).build());
        when(ventaRepository.findByEstadoAndFechaHoraAnulacionBetween(eq(EstadoVenta.ANULADA), any(), any())).thenReturn(List.of(venta));
        when(movimientoRepository.findByAnuladoTrueAndFechaHoraAnulacionBetween(any(), any())).thenReturn(List.of(
                movimiento(10L, null, null, AYER.plusHours(1)),        // manual: entra
                movimiento(11L, 1L, "FV-000001", AYER),                 // pago de la factura: ya está con la factura
                movimiento(12L, null, "VTA-2", AYER.plusHours(2))));    // ingreso de la venta: ya está con la venta

        List<AnulacionesService.Anulacion> lista = service.listar(LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(lista).extracting(AnulacionesService.Anulacion::tipo).containsExactly("VENTA", "EGRESO", "FACTURA");
        assertThat(lista.get(0).mismaPersona()).isTrue();     // la venta la registró y la anuló admin
        assertThat(lista.get(1).mismaPersona()).isFalse();    // el egreso lo registró finanzas
        assertThat(lista.get(2).referencia()).isEqualTo("FV-000001");
        assertThat(lista.get(0).descripcion()).contains("Gaseosa ×1");
    }
}
