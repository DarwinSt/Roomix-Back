package com.example.roomix.finanzas.service;

import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.finanzas.domain.GastoRecurrente;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.dto.GastoDtos.GastoRecurrenteRequest;
import com.example.roomix.finanzas.dto.GastoDtos.GastoRecurrenteResponse;
import com.example.roomix.finanzas.exception.FinanzasErrorCode;
import com.example.roomix.finanzas.exception.FinanzasException;
import com.example.roomix.finanzas.repository.GastoRecurrenteRepository;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Gastos recurrentes: cada mes generan un egreso pendiente («por pagar») con vencimiento el día configurado.
 * La generación se ejecuta al arrancar, todos los días a las 00:15 y al crear o consultar las plantillas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GastoRecurrenteService {

    private final GastoRecurrenteRepository repository;
    private final MovimientoFinancieroRepository movimientoRepository;

    @Transactional
    public List<GastoRecurrenteResponse> listar() {
        generarPendientes();
        return repository.findAllByOrderByActivoDescDiaVencimientoAsc().stream().map(this::respuesta).toList();
    }

    @Transactional
    public GastoRecurrenteResponse crear(GastoRecurrenteRequest request) {
        GastoRecurrente g = new GastoRecurrente();
        g.setCreadoPor(UsuarioActual.username());
        aplicar(g, request);
        repository.save(g);
        generar(g, LocalDate.now());
        return respuesta(g);
    }

    @Transactional
    public GastoRecurrenteResponse actualizar(Long id, GastoRecurrenteRequest request) {
        GastoRecurrente g = buscar(id);
        aplicar(g, request);
        repository.save(g);
        actualizarPendienteDelMes(g);
        return respuesta(g);
    }

    /** El egreso del mes en curso que aún no se ha pagado toma los datos nuevos del gasto (los ya pagados no cambian). */
    private void actualizarPendienteDelMes(GastoRecurrente g) {
        String periodo = YearMonth.now().toString();
        for (MovimientoFinanciero m : movimientoRepository.findByGastoRecurrenteIdAndPendienteTrueAndAnuladoFalse(g.getId())) {
            if (!m.getConcepto().endsWith(" — " + periodo)) {
                continue;
            }
            LocalDate vence = YearMonth.now().atDay(g.getDiaVencimiento());
            m.setConcepto(g.getConcepto() + " — " + periodo);
            m.setCategoria(g.getCategoria());
            m.setValor(g.getValor().setScale(2, RoundingMode.HALF_UP));
            m.setFecha(vence);
            m.setFechaVencimiento(vence);
            m.setMetodoPago(g.getMetodoPago());
            m.setTercero(g.getTercero());
            movimientoRepository.save(m);
        }
    }

    @Transactional
    public GastoRecurrenteResponse cambiarActivo(Long id, boolean activo) {
        GastoRecurrente g = buscar(id);
        g.setActivo(activo);
        repository.save(g);
        if (activo) {
            generar(g, LocalDate.now());
        }
        return respuesta(g);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 15 0 * * *")
    @Transactional
    public void generarPendientes() {
        LocalDate hoy = LocalDate.now();
        for (GastoRecurrente g : repository.findByActivoTrue()) {
            generar(g, hoy);
        }
    }

    /** Genera el egreso pendiente del mes en curso si aún no existe (idempotente por periodo). */
    private void generar(GastoRecurrente g, LocalDate hoy) {
        if (!g.isActivo()) {
            return;
        }
        String periodo = YearMonth.from(hoy).toString();
        if (periodo.equals(g.getUltimoPeriodo())) {
            return;
        }
        LocalDate vence = YearMonth.from(hoy).atDay(g.getDiaVencimiento());
        movimientoRepository.save(MovimientoFinanciero.builder()
                .tipo(TipoMovimiento.EGRESO)
                .categoria(g.getCategoria())
                .concepto(g.getConcepto() + " — " + periodo)
                .valor(g.getValor().setScale(2, RoundingMode.HALF_UP))
                .fecha(vence)
                .fechaVencimiento(vence)
                .pendiente(true)
                .metodoPago(g.getMetodoPago())
                .tercero(g.getTercero())
                .gastoRecurrenteId(g.getId())
                .registradoPor(g.getCreadoPor() != null ? g.getCreadoPor() : "sistema")
                .build());
        g.setUltimoPeriodo(periodo);
        repository.save(g);
        log.info("Gasto recurrente «{}» generado como pendiente para {}", g.getConcepto(), periodo);
    }

    private void aplicar(GastoRecurrente g, GastoRecurrenteRequest r) {
        if (r.categoria().getTipo() != TipoMovimiento.EGRESO) {
            throw new FinanzasException(FinanzasErrorCode.CATEGORIA_NO_COINCIDE, r.categoria().getNombre(), "egreso");
        }
        g.setConcepto(r.concepto().trim());
        g.setCategoria(r.categoria());
        g.setValor(r.valor());
        g.setDiaVencimiento(r.diaVencimiento());
        g.setTercero(StringUtils.hasText(r.tercero()) ? r.tercero().trim() : null);
        g.setMetodoPago(r.metodoPago());
        if (r.activo() != null) {
            g.setActivo(r.activo());
        }
    }

    private GastoRecurrenteResponse respuesta(GastoRecurrente g) {
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);
        boolean generadoEsteMes = mes.toString().equals(g.getUltimoPeriodo());
        LocalDate proximo = g.isActivo() ? (generadoEsteMes ? mes.plusMonths(1) : mes).atDay(g.getDiaVencimiento()) : null;
        return GastoRecurrenteResponse.from(g, proximo);
    }

    private GastoRecurrente buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new FinanzasException(FinanzasErrorCode.GASTO_RECURRENTE_NO_ENCONTRADO, id));
    }
}
