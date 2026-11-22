package com.example.roomix.venta.service;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.repository.FacturaItemRepository;
import com.example.roomix.finanzas.service.FinanzasService;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.inventario.domain.ArticuloInventario;
import com.example.roomix.inventario.dto.AjustarStockRequest;
import com.example.roomix.inventario.repository.ArticuloInventarioRepository;
import com.example.roomix.inventario.service.InventarioService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.venta.domain.ModoVenta;
import com.example.roomix.venta.domain.VentaProducto;
import com.example.roomix.venta.dto.VentaDtos.ItemVentaRequest;
import com.example.roomix.venta.dto.VentaDtos.VentaRequest;
import com.example.roomix.venta.dto.VentaDtos.VentaResponse;
import com.example.roomix.venta.exception.VentaException;
import com.example.roomix.venta.repository.VentaProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas de la venta de productos, sin base de datos: validaciones, totales, stock y registro del ingreso. */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaProductoRepository ventaRepository;
    @Mock
    private ArticuloInventarioRepository articuloRepository;
    @Mock
    private InventarioService inventarioService;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private FinanzasService finanzasService;
    @Mock
    private FacturaItemRepository facturaItemRepository;
    @InjectMocks
    private VentaService service;

    private static ArticuloInventario gaseosa(int stock, String precioVenta) {
        return ArticuloInventario.builder().id(1L).nombre("Gaseosa").cantidad(stock).activo(true)
                .precioUnitario(new BigDecimal("2000")).precioVenta(precioVenta == null ? null : new BigDecimal(precioVenta)).build();
    }

    private static VentaRequest contado(MetodoPago metodo, ItemVentaRequest... items) {
        return new VentaRequest(ModoVenta.CONTADO, null, List.of(items), metodo, null, null);
    }

    private void guardarConId() {
        when(ventaRepository.saveAndFlush(any())).thenAnswer(inv -> {
            VentaProducto v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });
    }

    @Test
    void sumaElMismoProductoDescuentaUnaVezYRegistraElIngreso() {
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(gaseosa(10, "4500")));
        guardarConId();
        when(finanzasService.registrarAutomatico(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(MovimientoFinanciero.builder().id(99L).build());

        VentaResponse v = service.registrar(contado(MetodoPago.EFECTIVO, new ItemVentaRequest(1L, 2), new ItemVentaRequest(1L, 3)));

        assertThat(v.total()).isEqualByComparingTo("22500");
        assertThat(v.items()).hasSize(1);
        assertThat(v.items().get(0).cantidad()).isEqualTo(5);
        ArgumentCaptor<AjustarStockRequest> salida = ArgumentCaptor.forClass(AjustarStockRequest.class);
        verify(inventarioService, times(1)).ajustarStock(eq(1L), salida.capture());
        assertThat(salida.getValue().cantidad()).isEqualTo(5);
        verify(finanzasService).registrarAutomatico(eq(CategoriaFinanciera.VENTA_PRODUCTOS), any(), eq(new BigDecimal("22500.00")),
                eq(MetodoPago.EFECTIVO), any(), eq("VTA-7"), any(), any());
    }

    @Test
    void guardaElCostoDelMomentoParaCalcularElMargen() {
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(gaseosa(10, "4500")));
        guardarConId();
        when(finanzasService.registrarAutomatico(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(MovimientoFinanciero.builder().id(99L).build());

        service.registrar(contado(MetodoPago.EFECTIVO, new ItemVentaRequest(1L, 1)));

        ArgumentCaptor<VentaProducto> venta = ArgumentCaptor.forClass(VentaProducto.class);
        verify(ventaRepository).saveAndFlush(venta.capture());
        assertThat(venta.getValue().getItems().get(0).getCostoUnitario()).isEqualByComparingTo("2000");
    }

    @Test
    void deContadoExigeMedioDePago() {
        assertThatThrownBy(() -> service.registrar(contado(null, new ItemVentaRequest(1L, 1))))
                .isInstanceOf(VentaException.class).extracting("errorCode").isEqualTo("VTA-006");
        verify(inventarioService, never()).ajustarStock(anyLong(), any());
    }

    @Test
    void noVendeMasDeLoQueHayNiProductosSinPrecio() {
        when(articuloRepository.findById(1L)).thenReturn(Optional.of(gaseosa(2, "4500")));
        assertThatThrownBy(() -> service.registrar(contado(MetodoPago.EFECTIVO, new ItemVentaRequest(1L, 3))))
                .isInstanceOf(VentaException.class).extracting("errorCode").isEqualTo("VTA-003");

        when(articuloRepository.findById(1L)).thenReturn(Optional.of(gaseosa(10, null)));
        assertThatThrownBy(() -> service.registrar(contado(MetodoPago.EFECTIVO, new ItemVentaRequest(1L, 1))))
                .isInstanceOf(VentaException.class).extracting("errorCode").isEqualTo("VTA-002");
        verify(ventaRepository, never()).saveAndFlush(any());
    }

    @Test
    void aLaHabitacionSoloConHuespedAlojado() {
        assertThatThrownBy(() -> service.registrar(new VentaRequest(ModoVenta.CARGO_HABITACION, null,
                List.of(new ItemVentaRequest(1L, 1)), null, null, null)))
                .isInstanceOf(VentaException.class).extracting("errorCode").isEqualTo("VTA-004");

        Reserva confirmada = Reserva.builder().id(5L).estadoReserva(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(confirmada));
        assertThatThrownBy(() -> service.registrar(new VentaRequest(ModoVenta.CARGO_HABITACION, 5L,
                List.of(new ItemVentaRequest(1L, 1)), null, null, null)))
                .isInstanceOf(VentaException.class).extracting("errorCode").isEqualTo("VTA-005");
    }
}
