package com.example.roomix.caja.service;

import com.example.roomix.auth.domain.Usuario;
import com.example.roomix.auth.repository.UsuarioRepository;
import com.example.roomix.auth.service.UsuarioActual;
import com.example.roomix.caja.domain.ConteoDenominacion;
import com.example.roomix.caja.domain.EstadoTurno;
import com.example.roomix.caja.domain.MovimientoCaja;
import com.example.roomix.caja.domain.TipoMovimientoCaja;
import com.example.roomix.caja.domain.TotalMedioPago;
import com.example.roomix.caja.domain.TurnoCaja;
import com.example.roomix.caja.dto.CajaDtos.AbrirTurnoRequest;
import com.example.roomix.caja.dto.CajaDtos.CerrarTurnoRequest;
import com.example.roomix.caja.dto.CajaDtos.ConteoItem;
import com.example.roomix.caja.dto.CajaDtos.DetalleTurno;
import com.example.roomix.caja.dto.CajaDtos.EstadoCaja;
import com.example.roomix.caja.dto.CajaDtos.MovimientoCajaRequest;
import com.example.roomix.caja.dto.CajaDtos.MovimientoInternoRequest;
import com.example.roomix.caja.dto.CajaDtos.MovimientoTurno;
import com.example.roomix.caja.dto.CajaDtos.TotalMedio;
import com.example.roomix.caja.dto.CajaDtos.TurnoResponse;
import com.example.roomix.caja.exception.CajaErrorCode;
import com.example.roomix.caja.exception.CajaException;
import com.example.roomix.caja.repository.MovimientoCajaRepository;
import com.example.roomix.caja.repository.TurnoCajaRepository;
import com.example.roomix.common.dto.PaginaResponse;
import com.example.roomix.finanzas.domain.MetodoPago;
import com.example.roomix.finanzas.domain.MovimientoFinanciero;
import com.example.roomix.finanzas.domain.TipoMovimiento;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoRequest;
import com.example.roomix.finanzas.dto.FinanzasDtos.MovimientoResponse;
import com.example.roomix.finanzas.repository.MovimientoFinancieroRepository;
import com.example.roomix.finanzas.service.FinanzasService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Caja de la recepción por turnos: apertura con base, pagos e ingresos/egresos del turno, retiros y aportes de
 * efectivo, arqueo y cierre con entrega. Los pagos, abonos y devoluciones que registra el cajero en otros módulos
 * entran solos al turno (ver {@link CajaTurnoResolver}); aquí no se duplica la contabilidad: el turno lee los
 * movimientos de Finanzas asociados a él.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajaService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.forLanguageTag("es-CO"));

    private final TurnoCajaRepository turnos;
    private final MovimientoCajaRepository internos;
    private final MovimientoFinancieroRepository movimientos;
    private final UsuarioRepository usuarios;
    private final FinanzasService finanzasService;

    public EstadoCaja estado() {
        var abierto = turnos.findFirstByEstado(EstadoTurno.ABIERTO);
        var ultimo = turnos.findFirstByEstadoOrderByFechaHoraCierreDesc(EstadoTurno.CERRADO);
        boolean esMio = abierto.map(t -> t.getCajero().equalsIgnoreCase(UsuarioActual.username())).orElse(false);
        return new EstadoCaja(abierto.map(this::respuesta).orElse(null), esMio,
                ultimo.map(TurnoCaja::getBaseSiguiente).orElse(null),
                ultimo.map(TurnoCaja::getCajeroNombre).orElse(null),
                ultimo.map(TurnoCaja::getFechaHoraCierre).orElse(null));
    }

    @Transactional
    public TurnoResponse abrir(AbrirTurnoRequest request) {
        turnos.findFirstByEstado(EstadoTurno.ABIERTO).ifPresent(t -> {
            throw new CajaException(CajaErrorCode.CAJA_OCUPADA, t.getCajeroNombre(), FECHA_HORA.format(t.getFechaHoraApertura()));
        });
        BigDecimal base = escala(request.baseInicial());
        BigDecimal esperada = turnos.findFirstByEstadoOrderByFechaHoraCierreDesc(EstadoTurno.CERRADO)
                .map(TurnoCaja::getBaseSiguiente).orElse(null);
        String observacion = texto(request.observacion());
        if (esperada != null && esperada.compareTo(base) != 0 && observacion == null) {
            throw new CajaException(CajaErrorCode.OBSERVACION_REQUERIDA, "La base recibida (" + pesos(base)
                    + ") es distinta de la que dejó el turno anterior (" + pesos(esperada) + ")");
        }
        String usuario = UsuarioActual.username();
        TurnoCaja turno = turnos.save(TurnoCaja.builder()
                .cajero(usuario)
                .cajeroNombre(usuarios.findByUsernameIgnoreCase(usuario).map(Usuario::getNombreCompleto).orElse(usuario))
                .estado(EstadoTurno.ABIERTO)
                .fechaHoraApertura(LocalDateTime.now())
                .baseInicial(base)
                .baseEsperada(esperada)
                .observacionApertura(observacion)
                .build());
        return respuesta(turno);
    }

    /** Ingreso o egreso del hotel registrado desde la caja: queda en Finanzas y en el turno. */
    @Transactional
    public MovimientoResponse registrarMovimiento(MovimientoCajaRequest request) {
        turnoPropioAbierto();
        return finanzasService.registrarManual(new MovimientoRequest(request.tipo(), request.categoria(),
                request.concepto(), request.valor(), LocalDate.now(), request.metodoPago(), request.tercero(), request.referencia()));
    }

    /** Retiro (a la caja fuerte o a la administración) o aporte de efectivo: no son ingresos ni egresos del hotel. */
    @Transactional
    public TurnoResponse registrarInterno(MovimientoInternoRequest request) {
        TurnoCaja turno = abiertoConPermiso();
        BigDecimal valor = escala(request.valor());
        if (request.tipo() == TipoMovimientoCaja.RETIRO) {
            BigDecimal enCaja = calcular(turno).efectivoEsperado();
            if (valor.compareTo(enCaja) > 0) {
                throw new CajaException(CajaErrorCode.RETIRO_EXCEDE_EFECTIVO, pesos(valor), pesos(enCaja));
            }
        }
        internos.save(MovimientoCaja.builder()
                .turno(turno)
                .tipo(request.tipo())
                .valor(valor)
                .motivo(request.motivo().trim())
                .fechaHora(LocalDateTime.now())
                .registradoPor(UsuarioActual.username())
                .build());
        return respuesta(turno);
    }

    /** Arqueo y cierre: compara el efectivo contado con el esperado, deja la base y registra la entrega. */
    @Transactional
    public TurnoResponse cerrar(CerrarTurnoRequest request) {
        TurnoCaja turno = abiertoConPermiso();
        boolean porAdministrador = !turno.getCajero().equalsIgnoreCase(UsuarioActual.username());
        String observacion = texto(request.observacion());
        if (porAdministrador && observacion == null) {
            throw new CajaException(CajaErrorCode.OBSERVACION_REQUERIDA, "El turno de " + turno.getCajeroNombre()
                    + " lo cierra un administrador");
        }
        List<ConteoDenominacion> conteo = new ArrayList<>();
        BigDecimal contado;
        if (request.conteo() != null && request.conteo().stream().anyMatch(c -> c.cantidad() > 0)) {
            contado = BigDecimal.ZERO;
            for (ConteoItem c : request.conteo()) {
                if (c.cantidad() > 0) {
                    conteo.add(new ConteoDenominacion(c.denominacion(), c.cantidad()));
                    contado = contado.add(BigDecimal.valueOf((long) c.denominacion() * c.cantidad()));
                }
            }
            contado = escala(contado);
        } else if (request.efectivoContado() != null) {
            contado = escala(request.efectivoContado());
        } else {
            throw new CajaException(CajaErrorCode.CONTEO_REQUERIDO);
        }
        Calculo calc = calcular(turno);
        BigDecimal diferencia = contado.subtract(calc.efectivoEsperado());
        if (diferencia.signum() != 0 && observacion == null) {
            throw new CajaException(CajaErrorCode.OBSERVACION_REQUERIDA, (diferencia.signum() < 0 ? "Hay un faltante de "
                    : "Hay un sobrante de ") + pesos(diferencia.abs()) + " frente al efectivo esperado (" + pesos(calc.efectivoEsperado()) + ")");
        }
        BigDecimal baseSiguiente = escala(request.baseSiguiente());
        if (baseSiguiente.compareTo(contado) > 0) {
            throw new CajaException(CajaErrorCode.BASE_EXCEDE_CONTADO, pesos(baseSiguiente), pesos(contado));
        }
        turno.setEstado(EstadoTurno.CERRADO);
        turno.setFechaHoraCierre(LocalDateTime.now());
        turno.setCerradoPor(UsuarioActual.username());
        turno.setEfectivoEsperado(calc.efectivoEsperado());
        turno.setEfectivoContado(contado);
        turno.setDiferencia(diferencia);
        turno.setBaseSiguiente(baseSiguiente);
        turno.setValorEntregado(contado.subtract(baseSiguiente));
        turno.setRecibidoPor(texto(request.recibidoPor()));
        turno.setObservacionCierre(observacion);
        turno.getConteo().clear();
        turno.getConteo().addAll(conteo);
        turno.getTotalesCierre().clear();
        calc.porMedio().forEach((m, v) -> turno.getTotalesCierre().add(new TotalMedioPago(m, v[0], v[1])));
        turno.setRetirosCierre(calc.retiros());
        turno.setAportesCierre(calc.aportes());
        turno.setMovimientosCierre(calc.cantidad());
        return respuesta(turnos.save(turno));
    }

    public DetalleTurno detalle(Long id) {
        TurnoCaja turno = buscar(id);
        validarConsulta(turno);
        List<MovimientoTurno> lista = new ArrayList<>();
        for (MovimientoFinanciero m : movimientos.findByTurnoCajaIdOrderByFechaHoraCreacionAsc(id)) {
            MetodoPago metodo = m.getMetodoPago() != null ? m.getMetodoPago() : MetodoPago.OTRO;
            lista.add(new MovimientoTurno("FINANZAS", m.getId(), m.getFechaHoraCreacion(), m.getTipo().name(), m.getConcepto(),
                    m.getCategoria().getNombre(), metodo, metodo.getNombre(), m.getValor(), m.getReferencia(), m.getFacturaId(),
                    m.getRegistradoPor(), m.isAnulado()));
        }
        for (MovimientoCaja c : internos.findByTurnoIdOrderByFechaHoraAsc(id)) {
            lista.add(new MovimientoTurno("CAJA", c.getId(), c.getFechaHora(), c.getTipo().name(), c.getMotivo(),
                    c.getTipo().getNombre(), MetodoPago.EFECTIVO, MetodoPago.EFECTIVO.getNombre(), c.getValor(), null, null,
                    c.getRegistradoPor(), false));
        }
        lista.sort(Comparator.comparing(MovimientoTurno::fechaHora, Comparator.nullsLast(Comparator.naturalOrder())));
        return new DetalleTurno(respuesta(turno), lista);
    }

    /** Historial de turnos; la recepción solo ve los suyos. */
    public PaginaResponse<TurnoResponse> historial(LocalDate desde, LocalDate hasta, String cajero, int pagina, int tamano) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(30);
        String filtro = UsuarioActual.tieneRol("RECEPCION") ? UsuarioActual.username() : texto(cajero);
        return PaginaResponse.de(turnos.buscar(filtro, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100))), this::respuesta);
    }

    // ------------------------------------------------------------------ Internos

    private record Calculo(Map<MetodoPago, BigDecimal[]> porMedio, BigDecimal retiros, BigDecimal aportes,
                           BigDecimal efectivoEsperado, int cantidad) {
    }

    /** Totales en vivo del turno a partir de los movimientos de Finanzas asociados y los retiros/aportes. */
    private Calculo calcular(TurnoCaja turno) {
        Map<MetodoPago, BigDecimal[]> porMedio = new EnumMap<>(MetodoPago.class);
        int cantidad = 0;
        for (MovimientoFinanciero m : movimientos.findByTurnoCajaIdOrderByFechaHoraCreacionAsc(turno.getId())) {
            if (m.isAnulado() || m.isPendiente()) {
                continue;
            }
            BigDecimal[] v = porMedio.computeIfAbsent(m.getMetodoPago() != null ? m.getMetodoPago() : MetodoPago.OTRO,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int i = m.getTipo() == TipoMovimiento.INGRESO ? 0 : 1;
            v[i] = v[i].add(m.getValor());
            cantidad++;
        }
        BigDecimal retiros = BigDecimal.ZERO;
        BigDecimal aportes = BigDecimal.ZERO;
        for (MovimientoCaja c : internos.findByTurnoIdOrderByFechaHoraAsc(turno.getId())) {
            if (c.getTipo() == TipoMovimientoCaja.RETIRO) {
                retiros = retiros.add(c.getValor());
            } else {
                aportes = aportes.add(c.getValor());
            }
            cantidad++;
        }
        BigDecimal[] ef = porMedio.getOrDefault(MetodoPago.EFECTIVO, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        BigDecimal esperado = turno.getBaseInicial().add(ef[0]).subtract(ef[1]).add(aportes).subtract(retiros);
        return new Calculo(porMedio, escala(retiros), escala(aportes), escala(esperado), cantidad);
    }

    private TurnoResponse respuesta(TurnoCaja t) {
        Map<MetodoPago, BigDecimal[]> porMedio;
        BigDecimal retiros;
        BigDecimal aportes;
        BigDecimal esperado;
        int cantidad;
        if (t.abierto()) {
            Calculo c = calcular(t);
            porMedio = c.porMedio();
            retiros = c.retiros();
            aportes = c.aportes();
            esperado = c.efectivoEsperado();
            cantidad = c.cantidad();
        } else {
            porMedio = new EnumMap<>(MetodoPago.class);
            for (TotalMedioPago tm : t.getTotalesCierre()) {
                porMedio.put(tm.getMetodo(), new BigDecimal[]{tm.getIngresos(), tm.getEgresos()});
            }
            retiros = cero(t.getRetirosCierre());
            aportes = cero(t.getAportesCierre());
            esperado = t.getEfectivoEsperado();
            cantidad = t.getMovimientosCierre() != null ? t.getMovimientosCierre() : 0;
        }
        List<TotalMedio> totales = new ArrayList<>();
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (var e : porMedio.entrySet()) {
            BigDecimal in = escala(e.getValue()[0]);
            BigDecimal out = escala(e.getValue()[1]);
            totales.add(new TotalMedio(e.getKey(), e.getKey().getNombre(), in, out, in.subtract(out)));
            ingresos = ingresos.add(in);
            egresos = egresos.add(out);
        }
        BigDecimal[] ef = porMedio.getOrDefault(MetodoPago.EFECTIVO, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        List<ConteoItem> conteo = t.getConteo().stream().map(c -> new ConteoItem(c.getDenominacion(), c.getCantidad())).toList();
        return new TurnoResponse(t.getId(), t.getCajero(), t.getCajeroNombre(), t.getEstado(), t.getFechaHoraApertura(),
                t.getBaseInicial(), t.getBaseEsperada(), t.getObservacionApertura(), t.getFechaHoraCierre(), t.getCerradoPor(),
                totales, escala(ingresos), escala(egresos), escala(ef[0]), escala(ef[1]), retiros, aportes, esperado,
                t.getEfectivoContado(), t.getDiferencia(), t.getBaseSiguiente(), t.getValorEntregado(), t.getRecibidoPor(),
                t.getObservacionCierre(), conteo, cantidad);
    }

    private TurnoCaja turnoPropioAbierto() {
        var abierto = turnos.findFirstByEstado(EstadoTurno.ABIERTO)
                .orElseThrow(() -> new CajaException(CajaErrorCode.TURNO_REQUERIDO));
        if (!abierto.getCajero().equalsIgnoreCase(UsuarioActual.username())) {
            throw new CajaException(CajaErrorCode.CAJA_DE_OTRO, abierto.getCajeroNombre());
        }
        return abierto;
    }

    /** Turno abierto sobre el que puede operar el usuario: su cajero o un administrador. */
    private TurnoCaja abiertoConPermiso() {
        TurnoCaja turno = turnos.findFirstByEstado(EstadoTurno.ABIERTO)
                .orElseThrow(() -> new CajaException(CajaErrorCode.TURNO_REQUERIDO));
        if (!turno.getCajero().equalsIgnoreCase(UsuarioActual.username()) && !UsuarioActual.tieneRol("ADMIN")) {
            throw new CajaException(CajaErrorCode.SOLO_CAJERO, turno.getCajeroNombre());
        }
        return turno;
    }

    private void validarConsulta(TurnoCaja turno) {
        if (UsuarioActual.tieneRol("RECEPCION") && !turno.getCajero().equalsIgnoreCase(UsuarioActual.username())) {
            throw new CajaException(CajaErrorCode.SOLO_CAJERO, turno.getCajeroNombre());
        }
    }

    private TurnoCaja buscar(Long id) {
        return turnos.findById(id).orElseThrow(() -> new CajaException(CajaErrorCode.TURNO_NO_ENCONTRADO, id));
    }

    private static BigDecimal escala(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cero(BigDecimal v) {
        return escala(v);
    }

    private static String texto(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String pesos(BigDecimal v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return f.format(v);
    }
}
