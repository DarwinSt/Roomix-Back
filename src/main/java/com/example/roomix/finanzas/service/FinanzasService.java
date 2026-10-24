package com.example.roomix.finanzas.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.domain.CategoriaFinanciera;
import com.example.roomix.finanzas.domain.EstadoFactura;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.dto.FinanzasDtos.CategoriaResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.Comparativo;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoResponse;
import com.example.roomix.finanzas.dto.FinanzasDtos.PuntoSerie;
import com.example.roomix.finanzas.dto.FinanzasDtos.ResumenFinanciero;
import com.example.roomix.finanzas.dto.FinanzasDtos.TotalCategoria;
import com.example.roomix.finanzas.exception.FinanzasErrorCode;
import com.example.roomix.finanzas.exception.FinanzasException;
import com.example.roomix.finanzas.repository.FacturaRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registro de ingresos y egresos y reportes financieros (estado de resultados, flujo, comparativos).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanzasService {

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private final MovimientoFinancieroRepository movimientoRepository;
    private final FacturaRepository facturaRepository;

    public PaginaResponse<MovimientoResponse> listar(
            TipoMovimiento tipo,
            CategoriaFinanciera categoria,
            LocalDate desde,
            LocalDate hasta,
            boolean incluirAnulados,
            String texto,
            int pagina,
            int tamano
    ) {
        return PaginaResponse.de(
                movimientoRepository.findAll(
                        MovimientoFinancieroRepository.conFiltros(tipo, categoria, desde, hasta, incluirAnulados, texto),
                        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 200),
                                Sort.by(Sort.Direction.DESC, "fecha", "id"))
                ),
                MovimientoResponse::from
        );
    }

    public List<CategoriaResponse> categorias() {
        return Arrays.stream(CategoriaFinanciera.values())
                .map(c -> new CategoriaResponse(c, c.getNombre(), c.getTipo()))
                .toList();
    }

    @Transactional
    public MovimientoResponse registrarManual(MovimientoRequest request) {
        if (request.categoria().getTipo() != request.tipo()) {
            throw new FinanzasException(FinanzasErrorCode.CATEGORIA_NO_COINCIDE,
                    request.categoria().getNombre(), request.tipo().name().toLowerCase());
        }
        MovimientoFinanciero movimiento = MovimientoFinanciero.builder()
                .tipo(request.tipo())
                .categoria(request.categoria())
                .concepto(request.concepto().trim())
                .valor(request.valor().setScale(2, RoundingMode.HALF_UP))
                .fecha(request.fecha() != null ? request.fecha() : LocalDate.now())
                .metodoPago(request.metodoPago())
                .tercero(textoOpcional(request.tercero()))
                .referencia(textoOpcional(request.referencia()))
                .registradoPor(UsuarioActual.username())
                .build();
        return MovimientoResponse.from(movimientoRepository.save(movimiento));
    }

    /** Registro automático desde otros módulos (pagos de facturas, compras de inventario). */
    @Transactional
    public MovimientoFinanciero registrarAutomatico(
            CategoriaFinanciera categoria,
            String concepto,
            BigDecimal valor,
            MetodoPago metodoPago,
            String tercero,
            String referencia,
            Long facturaId,
            Long pagoId
    ) {
        return movimientoRepository.save(MovimientoFinanciero.builder()
                .tipo(categoria.getTipo())
                .categoria(categoria)
                .concepto(concepto)
                .valor(valor.setScale(2, RoundingMode.HALF_UP))
                .fecha(LocalDate.now())
                .metodoPago(metodoPago)
                .tercero(tercero)
                .referencia(referencia)
                .facturaId(facturaId)
                .pagoId(pagoId)
                .registradoPor(UsuarioActual.username())
                .build());
    }

    @Transactional
    public MovimientoResponse anular(Long id, String motivo) {
        MovimientoFinanciero movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new FinanzasException(FinanzasErrorCode.MOVIMIENTO_NO_ENCONTRADO, id));
        if (movimiento.isAnulado()) {
            throw new FinanzasException(FinanzasErrorCode.MOVIMIENTO_YA_ANULADO);
        }
        if (movimiento.getPagoId() != null) {
            throw new FinanzasException(FinanzasErrorCode.MOVIMIENTO_AUTOMATICO, movimiento.getReferencia());
        }
        movimiento.setAnulado(true);
        movimiento.setMotivoAnulacion(motivo.trim());
        return MovimientoResponse.from(movimientoRepository.save(movimiento));
    }

    /** Anula los ingresos generados por un pago (al anular el pago o la factura). */
    @Transactional
    public void anularPorPago(Long pagoId, String motivo) {
        for (MovimientoFinanciero m : movimientoRepository.findByPagoIdAndAnuladoFalse(pagoId)) {
            m.setAnulado(true);
            m.setMotivoAnulacion(motivo);
            movimientoRepository.save(m);
        }
    }

    public ResumenFinanciero resumen(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
        if (inicio.isAfter(fin)) {
            LocalDate t = inicio;
            inicio = fin;
            fin = t;
        }

        Map<CategoriaFinanciera, BigDecimal> totales = totalesPorCategoria(inicio, fin);
        BigDecimal ingresos = sumar(totales, TipoMovimiento.INGRESO);
        BigDecimal egresos = sumar(totales, TipoMovimiento.EGRESO);
        BigDecimal utilidad = ingresos.subtract(egresos);

        long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
        LocalDate antFin = inicio.minusDays(1);
        LocalDate antInicio = antFin.minusDays(dias - 1);
        Map<CategoriaFinanciera, BigDecimal> totalesAnt = totalesPorCategoria(antInicio, antFin);
        BigDecimal ingresosAnt = sumar(totalesAnt, TipoMovimiento.INGRESO);
        BigDecimal egresosAnt = sumar(totalesAnt, TipoMovimiento.EGRESO);
        BigDecimal utilidadAnt = ingresosAnt.subtract(egresosAnt);

        String granularidad = dias > 62 ? "MES" : "DIA";
        return new ResumenFinanciero(
                inicio,
                fin,
                ingresos,
                egresos,
                utilidad,
                ingresos.signum() == 0 ? null : porcentaje(utilidad, ingresos),
                facturaRepository.saldoPendiente(List.of(EstadoFactura.PENDIENTE, EstadoFactura.PAGADA_PARCIAL)),
                granularidad,
                desglose(totales, TipoMovimiento.INGRESO, ingresos),
                desglose(totales, TipoMovimiento.EGRESO, egresos),
                serie(inicio, fin, granularidad),
                new Comparativo(antInicio, antFin, ingresosAnt, egresosAnt, utilidadAnt,
                        variacion(ingresos, ingresosAnt), variacion(egresos, egresosAnt), variacion(utilidad, utilidadAnt))
        );
    }

    /** Ingresos (no anulados) en un rango, usado por el dashboard. */
    public BigDecimal ingresosEntre(LocalDate desde, LocalDate hasta) {
        return sumar(totalesPorCategoria(desde, hasta), TipoMovimiento.INGRESO);
    }

    public Map<CategoriaFinanciera, BigDecimal> totalesPorCategoria(LocalDate desde, LocalDate hasta) {
        Map<CategoriaFinanciera, BigDecimal> mapa = new EnumMap<>(CategoriaFinanciera.class);
        for (Object[] fila : movimientoRepository.totalesPorCategoria(desde, hasta)) {
            mapa.put((CategoriaFinanciera) fila[0], (BigDecimal) fila[1]);
        }
        return mapa;
    }

    private List<PuntoSerie> serie(LocalDate inicio, LocalDate fin, String granularidad) {
        Map<LocalDate, BigDecimal[]> puntos = new LinkedHashMap<>();
        boolean mensual = "MES".equals(granularidad);
        LocalDate cursor = mensual ? inicio.withDayOfMonth(1) : inicio;
        while (!cursor.isAfter(fin)) {
            puntos.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = mensual ? cursor.plusMonths(1) : cursor.plusDays(1);
        }
        for (Object[] fila : movimientoRepository.totalesDiarios(inicio, fin)) {
            LocalDate fecha = (LocalDate) fila[0];
            LocalDate clave = mensual ? fecha.withDayOfMonth(1) : fecha;
            BigDecimal[] valores = puntos.computeIfAbsent(clave, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int idx = fila[1] == TipoMovimiento.INGRESO ? 0 : 1;
            valores[idx] = valores[idx].add((BigDecimal) fila[2]);
        }
        return puntos.entrySet().stream()
                .map(e -> new PuntoSerie(
                        mensual
                                ? e.getKey().getMonth().getDisplayName(TextStyle.SHORT, ES) + " " + e.getKey().getYear()
                                : e.getKey().getDayOfMonth() + " " + e.getKey().getMonth().getDisplayName(TextStyle.SHORT, ES),
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1]))
                .toList();
    }

    private static List<TotalCategoria> desglose(
            Map<CategoriaFinanciera, BigDecimal> totales,
            TipoMovimiento tipo,
            BigDecimal total
    ) {
        return totales.entrySet().stream()
                .filter(e -> e.getKey().getTipo() == tipo)
                .sorted(Map.Entry.<CategoriaFinanciera, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new TotalCategoria(e.getKey(), e.getKey().getNombre(), e.getValue(),
                        total.signum() == 0 ? 0 : porcentaje(e.getValue(), total)))
                .toList();
    }

    private static BigDecimal sumar(Map<CategoriaFinanciera, BigDecimal> totales, TipoMovimiento tipo) {
        return totales.entrySet().stream()
                .filter(e -> e.getKey().getTipo() == tipo)
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double porcentaje(BigDecimal parte, BigDecimal total) {
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue();
    }

    private static Double variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 1, RoundingMode.HALF_UP).doubleValue();
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
