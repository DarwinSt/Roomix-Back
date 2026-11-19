package com.example.roomix.reserva.service;

import com.example.roomix.auth.service.CorreoService;
import com.example.roomix.config.ConfiguracionHotel;
import com.example.roomix.config.ConfiguracionHotelService;
import com.example.roomix.finanzas.dto.AbonoDtos.ResumenAbonos;
import com.example.roomix.finanzas.service.AbonoService;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.domain.ReservaConfirmadaEvent;
import com.example.roomix.reserva.repository.ReservaRepository;
import com.example.roomix.reserva.service.PoliticaReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Envía al huésped el correo de confirmación de su reserva, si el hotel lo activó y el huésped tiene correo. Se arma
 * después de guardar la reserva y se envía en segundo plano: un fallo del correo nunca afecta la reserva.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorreoReservaService {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);

    private final ReservaRepository reservaRepository;
    private final AbonoService abonoService;
    private final PoliticaReservaService politicaService;
    private final ConfiguracionHotelService configuracion;
    private final CorreoService correo;
    private final ExecutorService envios = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "correo-reservas");
        t.setDaemon(true);
        return t;
    });

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alConfirmar(ReservaConfirmadaEvent evento) {
        try {
            if (!configuracion.correoConfirmacionReserva() || !correo.habilitado()) {
                return;
            }
            Reserva r = reservaRepository.findById(evento.reservaId()).orElse(null);
            if (r == null || r.getHuesped() == null || !StringUtils.hasText(r.getHuesped().getEmail())) {
                return;
            }
            ConfiguracionHotel hotel = configuracion.entidad();
            ResumenAbonos abonos = abonoService.resumen(r.getId());
            BigDecimal penalidad = politicaService.vigente().getPenalidadCancelacionPct();
            String destino = r.getHuesped().getEmail().trim();
            String nombre = r.getHuesped().nombreCompleto();
            String asunto = "Reserva confirmada #" + r.getId() + " — " + hotel.getNombre();
            String html = html(r, hotel, abonos, penalidad);
            String texto = texto(r, hotel, abonos, penalidad);
            envios.submit(() -> correo.enviarHtml(destino, nombre, hotel.getNombre(), asunto, html, texto));
        } catch (Exception e) {
            log.warn("No se pudo preparar el correo de la reserva {}: {}", evento.reservaId(), e.getMessage());
        }
    }

    private String html(Reserva r, ConfiguracionHotel h, ResumenAbonos a, BigDecimal penalidad) {
        StringBuilder filas = new StringBuilder();
        fila(filas, "Habitación", r.getHabitacion().getNumero() + " (" + r.getHabitacion().getTipoHabitacion().name().toLowerCase() + ")");
        fila(filas, "Llegada", FECHA.format(r.getFechaEntrada()));
        fila(filas, "Salida", FECHA.format(r.getFechaSalida()));
        fila(filas, "Noches", String.valueOf(r.getCantidadNoches()));
        fila(filas, "Personas", String.valueOf(r.personas()));
        fila(filas, "Total de la estadía", pesos(a.totalReserva()));
        fila(filas, "Abonado", pesos(a.totalAbonado()));
        fila(filas, "Saldo por pagar", pesos(a.saldo()));
        String contacto = String.join(" · ", java.util.stream.Stream.of(h.getDireccion(), h.getCiudad(), h.getTelefono(), h.getEmail())
                .filter(StringUtils::hasText).map(HtmlUtils::htmlEscape).toList());
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2a24">
                  <h2 style="color:#1f5c3d;margin:0 0 4px">%s</h2>
                  <p style="margin:0 0 16px;color:#5b6660">Confirmación de reserva #%d</p>
                  <p>Hola, %s:</p>
                  <p>Tu reserva está <strong>confirmada</strong>. Estos son los datos:</p>
                  <table style="border-collapse:collapse;width:100%%">%s</table>
                  %s
                  <p style="margin-top:20px">Te esperamos.</p>
                  <p style="font-size:12px;color:#5b6660;border-top:1px solid #e3e8e5;padding-top:10px">%s</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(h.getNombre()), r.getId(), HtmlUtils.htmlEscape(r.getHuesped().getNombre()),
                filas, penalidad.signum() > 0
                        ? "<p style=\"font-size:13px;color:#5b6660\">Si cancelas, el hotel retiene el " + penalidad.stripTrailingZeros().toPlainString()
                        + " %% de lo abonado y devuelve el resto.</p>" : "", contacto);
    }

    private String texto(Reserva r, ConfiguracionHotel h, ResumenAbonos a, BigDecimal penalidad) {
        return h.getNombre() + "\nConfirmación de reserva #" + r.getId() + "\n\nHola, " + r.getHuesped().getNombre()
                + ": tu reserva está confirmada.\n\nHabitación: " + r.getHabitacion().getNumero()
                + "\nLlegada: " + FECHA.format(r.getFechaEntrada()) + "\nSalida: " + FECHA.format(r.getFechaSalida())
                + "\nNoches: " + r.getCantidadNoches() + "\nPersonas: " + r.personas()
                + "\nTotal: " + pesos(a.totalReserva()) + "\nAbonado: " + pesos(a.totalAbonado()) + "\nSaldo: " + pesos(a.saldo())
                + (penalidad.signum() > 0 ? "\n\nSi cancelas, el hotel retiene el " + penalidad.stripTrailingZeros().toPlainString()
                + " % de lo abonado." : "") + "\n\nTe esperamos.";
    }

    private static void fila(StringBuilder sb, String etiqueta, String valor) {
        sb.append("<tr><td style=\"padding:6px 8px;border-bottom:1px solid #e3e8e5;color:#5b6660\">").append(etiqueta)
                .append("</td><td style=\"padding:6px 8px;border-bottom:1px solid #e3e8e5;text-align:right\"><strong>")
                .append(HtmlUtils.htmlEscape(valor)).append("</strong></td></tr>");
    }

    private static String pesos(BigDecimal v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(ES);
        f.setMaximumFractionDigits(0);
        return f.format(v == null ? BigDecimal.ZERO : v);
    }
}
