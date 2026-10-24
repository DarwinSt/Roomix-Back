package com.example.roomix.finanzas.service;

import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.ResumenFinanciero;
import com.example.roomix.finanzas.exception.FinanzasErrorCode;
import com.example.roomix.finanzas.exception.FinanzasException;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanzasServiceTest {

    @Mock
    private MovimientoFinancieroRepository movimientoRepository;
    @Mock
    private FacturaRepository facturaRepository;

    @InjectMocks
    private FinanzasService finanzasService;

    @Test
    void laCategoriaDebeCorresponderAlTipo() {
        MovimientoRequest request = new MovimientoRequest(TipoMovimiento.INGRESO, CategoriaFinanciera.NOMINA, "Pago",
                BigDecimal.TEN, null, null, null, null);
        assertThatThrownBy(() -> finanzasService.registrarManual(request))
                .isInstanceOfSatisfying(FinanzasException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(FinanzasErrorCode.CATEGORIA_NO_COINCIDE));
    }

    @Test
    void losIngresosDePagosSoloSeAnulanDesdeLaFactura() {
        when(movimientoRepository.findById(3L)).thenReturn(Optional.of(MovimientoFinanciero.builder()
                .id(3L).tipo(TipoMovimiento.INGRESO).categoria(CategoriaFinanciera.ALOJAMIENTO).pagoId(8L)
                .referencia("FV-000010").build()));

        assertThatThrownBy(() -> finanzasService.anular(3L, "error"))
                .isInstanceOfSatisfying(FinanzasException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(FinanzasErrorCode.MOVIMIENTO_AUTOMATICO));
    }

    @Test
    void resumenCalculaUtilidadMargenYVariacionContraElPeriodoAnterior() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 10);
        when(movimientoRepository.totalesPorCategoria(desde, hasta)).thenReturn(List.of(
                new Object[]{CategoriaFinanciera.ALOJAMIENTO, new BigDecimal("8000000")},
                new Object[]{CategoriaFinanciera.SERVICIOS, new BigDecimal("2000000")},
                new Object[]{CategoriaFinanciera.NOMINA, new BigDecimal("6000000")}));
        when(movimientoRepository.totalesPorCategoria(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 30))).thenReturn(List.<Object[]>of(
                new Object[]{CategoriaFinanciera.ALOJAMIENTO, new BigDecimal("5000000")},
                new Object[]{CategoriaFinanciera.NOMINA, new BigDecimal("4000000")}));
        when(movimientoRepository.totalesDiarios(eq(desde), eq(hasta))).thenReturn(List.of());
        when(facturaRepository.saldoPendiente(any())).thenReturn(BigDecimal.ZERO);

        ResumenFinanciero r = finanzasService.resumen(desde, hasta);

        assertThat(r.totalIngresos()).isEqualByComparingTo("10000000");
        assertThat(r.totalEgresos()).isEqualByComparingTo("6000000");
        assertThat(r.utilidad()).isEqualByComparingTo("4000000");
        assertThat(r.margenPct()).isEqualTo(40.0);
        assertThat(r.periodoAnterior().variacionIngresosPct()).isEqualTo(100.0);
        assertThat(r.serie()).hasSize(10);
        assertThat(r.ingresosPorCategoria().getFirst().categoria()).isEqualTo(CategoriaFinanciera.ALOJAMIENTO);
        assertThat(r.ingresosPorCategoria().getFirst().porcentaje()).isEqualTo(80.0);
    }
}
