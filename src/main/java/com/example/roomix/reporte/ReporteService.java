package com.example.roomix.reporte;

import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.habitacion.repository.HabitacionRepository;
import com.example.roomix.reporte.ReporteOcupacionResponse.Grupo;
import com.example.roomix.reporte.ReporteOcupacionResponse.PuntoDia;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.servicio.repository.SolicitudServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reportes de gestión hotelera calculados a partir de las reservas.
 * Las noches se cuentan día a día: una reserva del 3 al 5 ocupa las noches del 3 y del 4.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private static final List<EstadoReserva> VENDIDAS = List.of(
            EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);

    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitacionRepository;
    private final SolicitudServicioRepository solicitudRepository;

    public ReporteOcupacionResponse ocupacion(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (inicio.isAfter(fin)) {
            LocalDate t = inicio;
            inicio = fin;
            fin = t;
        }
        LocalDate finExclusivo = fin.plusDays(1);
        int habitaciones = (int) habitacionRepository.count();
        long dias = ChronoUnit.DAYS.between(inicio, finExclusivo);
        long nochesDisponibles = habitaciones * dias;

        List<Reserva> ocupan = reservaRepository.queOcupanRango(inicio, finExclusivo, VENDIDAS);

        // Serie diaria
        Map<LocalDate, long[]> noches = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> ingresos = new LinkedHashMap<>();
        for (LocalDate d = inicio; d.isBefore(finExclusivo); d = d.plusDays(1)) {
            noches.put(d, new long[]{0});
            ingresos.put(d, BigDecimal.ZERO);
        }
        Map<String, long[]> nochesPorTipo = new LinkedHashMap<>();
        Map<String, BigDecimal> ingresoPorTipo = new LinkedHashMap<>();
        for (Reserva r : ocupan) {
            LocalDate d0 = r.getFechaEntrada().isBefore(inicio) ? inicio : r.getFechaEntrada();
            LocalDate d1 = r.getFechaSalida().isAfter(finExclusivo) ? finExclusivo : r.getFechaSalida();
            String tipo = r.getHabitacion().getTipoHabitacion().name();
            for (LocalDate d = d0; d.isBefore(d1); d = d.plusDays(1)) {
                noches.get(d)[0]++;
                ingresos.merge(d, r.getTarifaNoche(), BigDecimal::add);
                nochesPorTipo.computeIfAbsent(tipo, k -> new long[]{0})[0]++;
                ingresoPorTipo.merge(tipo, r.getTarifaNoche(), BigDecimal::add);
            }
        }

        List<PuntoDia> serie = new ArrayList<>();
        long nochesVendidas = 0;
        BigDecimal ingresoTotal = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, long[]> e : noches.entrySet()) {
            long n = e.getValue()[0];
            BigDecimal ingreso = ingresos.get(e.getKey());
            nochesVendidas += n;
            ingresoTotal = ingresoTotal.add(ingreso);
            serie.add(new PuntoDia(e.getKey(), n, pct(n, habitaciones), ingreso));
        }

        // Reservas que llegan en el periodo (canal, procedencia, estancia y cancelaciones)
        LocalDate primerDia = inicio;
        LocalDate ultimoDia = fin;
        List<Reserva> llegadas = reservaRepository.findAll((root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("fechaEntrada"), primerDia),
                cb.lessThanOrEqualTo(root.get("fechaEntrada"), ultimoDia)));
        long cancelaciones = llegadas.stream().filter(r -> r.getEstadoReserva() == EstadoReserva.CANCELADA).count();
        List<Reserva> efectivas = llegadas.stream().filter(r -> r.getEstadoReserva() != EstadoReserva.CANCELADA).toList();
        double estancia = efectivas.isEmpty() ? 0
                : redondear(efectivas.stream().mapToInt(Reserva::getCantidadNoches).average().orElse(0));

        Map<String, long[]> canal = new LinkedHashMap<>();
        Map<String, BigDecimal> canalValor = new LinkedHashMap<>();
        Map<String, long[]> procedencia = new LinkedHashMap<>();
        Map<String, BigDecimal> procedenciaValor = new LinkedHashMap<>();
        for (Reserva r : efectivas) {
            String c = r.getCanal() != null ? r.getCanal().getNombre() : "Presencial";
            canal.computeIfAbsent(c, k -> new long[]{0})[0]++;
            canalValor.merge(c, r.getTotalEstimado(), BigDecimal::add);
            String nac = r.getHuesped().getNacionalidad();
            String p = nac == null || nac.isBlank() || nac.equalsIgnoreCase("Colombia") ? "Nacional (Colombia)" : nac;
            procedencia.computeIfAbsent(p, k -> new long[]{0})[0]++;
            procedenciaValor.merge(p, r.getTotalEstimado(), BigDecimal::add);
        }

        List<Grupo> servicios = solicitudRepository.ranking(inicio.atStartOfDay(), finExclusivo.atStartOfDay()).stream()
                .limit(10)
                .map(f -> new Grupo((String) f[0], ((Number) f[1]).longValue(), (BigDecimal) f[2]))
                .toList();

        return new ReporteOcupacionResponse(
                inicio, fin, habitaciones, nochesDisponibles, nochesVendidas,
                pct(nochesVendidas, nochesDisponibles),
                nochesVendidas == 0 ? BigDecimal.ZERO : ingresoTotal.divide(BigDecimal.valueOf(nochesVendidas), 0, RoundingMode.HALF_UP),
                nochesDisponibles == 0 ? BigDecimal.ZERO : ingresoTotal.divide(BigDecimal.valueOf(nochesDisponibles), 0, RoundingMode.HALF_UP),
                ingresoTotal,
                llegadas.size(),
                cancelaciones,
                pct(cancelaciones, llegadas.size()),
                estancia,
                serie,
                grupos(canal, canalValor),
                grupos(nochesPorTipo, ingresoPorTipo),
                grupos(procedencia, procedenciaValor),
                servicios
        );
    }

    private static List<Grupo> grupos(Map<String, long[]> conteo, Map<String, BigDecimal> valor) {
        return conteo.entrySet().stream()
                .map(e -> new Grupo(e.getKey(), e.getValue()[0], valor.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingLong(Grupo::cantidad).reversed())
                .toList();
    }

    private static double pct(long parte, long total) {
        return total == 0 ? 0 : redondear(parte * 100.0 / total);
    }

    private static double redondear(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
