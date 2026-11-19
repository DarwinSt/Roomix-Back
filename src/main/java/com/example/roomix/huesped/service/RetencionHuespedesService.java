package com.example.roomix.huesped.service;

import com.example.roomix.config.ConfiguracionHotelService;
import com.example.roomix.huesped.domain.Huesped;
import com.example.roomix.huesped.repository.HuespedRepository;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Política de retención (Ley 1581 de 2012): cuando un huésped lleva el número de meses configurado sin actividad
 * (sin reservas activas y sin estadías desde entonces), se anonimizan sus datos personales. Se conservan sus reservas y
 * facturas (las facturas guardan su propio nombre y documento, que la ley tributaria obliga a conservar) y la
 * nacionalidad, que sirve para las estadísticas de procedencia sin identificar a la persona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetencionHuespedesService {

    private static final List<EstadoReserva> ACTIVAS = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO);

    private final HuespedRepository huespedRepository;
    private final ReservaRepository reservaRepository;
    private final ConfiguracionHotelService configuracion;

    @Scheduled(cron = "0 30 3 * * *")
    public void tareaDiaria() {
        int n = anonimizarInactivos();
        if (n > 0) {
            log.info("Retención de datos: {} huésped(es) anonimizado(s)", n);
        }
    }

    /** Anonimiza los huéspedes inactivos según la configuración; devuelve cuántos se anonimizaron. */
    @Transactional
    public int anonimizarInactivos() {
        int meses = configuracion.mesesRetencionHuespedes();
        if (meses <= 0) {
            return 0;
        }
        LocalDate limite = LocalDate.now().minusMonths(meses);
        int total = 0;
        for (Huesped h : huespedRepository.findAll()) {
            if (Boolean.TRUE.equals(h.getAnonimizado())) {
                continue;
            }
            List<Reserva> reservas = reservaRepository.findByHuespedIdOrderByFechaEntradaDesc(h.getId());
            if (reservas.stream().anyMatch(r -> ACTIVAS.contains(r.getEstadoReserva()))) {
                continue;
            }
            LocalDate ultima = reservas.stream().map(Reserva::getFechaSalida).filter(Objects::nonNull)
                    .max(LocalDate::compareTo).orElse(null);
            LocalDate creado = h.getFechaHoraCreacion() != null ? h.getFechaHoraCreacion().toLocalDate() : LocalDate.now();
            LocalDate actividad = ultima != null && ultima.isAfter(creado) ? ultima : creado;
            if (actividad.isBefore(limite)) {
                anonimizar(h);
                total++;
            }
        }
        return total;
    }

    private void anonimizar(Huesped h) {
        h.setNombre("Huésped");
        h.setApellidos("anonimizado #" + h.getId());
        h.setNumeroDocumento("ANON-" + h.getId());
        h.setEmail("anonimizado-" + h.getId() + "@sin-datos.invalid");
        h.setTelefono("—");
        h.setFechaNacimiento(null);
        h.setNotas(null);
        h.setActivo(false);
        h.setAnonimizado(true);
        h.setFechaHoraAnonimizacion(LocalDateTime.now());
        huespedRepository.save(h);
    }
}
