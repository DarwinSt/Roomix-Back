package com.example.roomix.reserva.service;

import com.example.roomix.auth.service.CorreoService;
import com.example.roomix.config.ConfiguracionHotel;
import com.example.roomix.config.ConfiguracionHotelService;
import com.example.roomix.finanzas.dto.AbonoDtos.ResumenAbonos;
import com.example.roomix.finanzas.service.AbonoService;
import com.example.roomix.habitacion.domain.EstadoReserva;
import com.example.roomix.reserva.domain.Reserva;
import com.example.roomix.reserva.domain.ReservaCanceladaEvent;
import com.example.roomix.reserva.domain.ReservaConfirmadaEvent;
import com.example.roomix.reserva.domain.ReservaModificadaEvent;
import com.example.roomix.reserva.exception.ReservaErrorCode;
import com.example.roomix.reserva.exception.ReservaException;
import com.example.roomix.reserva.repository.ReservaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Correos al huésped sobre su reserva: confirmación al crearla, aviso si cambian las fechas o las personas y aviso de
 * cancelación. Se envían en segundo plano después de guardar (un fallo del correo nunca afecta la reserva) y el
 * resultado queda en la reserva (enviado, falló o sin correo) para poder reenviarlo desde su detalle.
 */
@Slf4j
@Service
public class CorreoReservaService {

    public enum TipoCorreo { CONFIRMACION, MODIFICACION, CANCELACION }

    public static final String ENVIADO = "ENVIADO";
    public static final String FALLIDO = "FALLIDO";
    public static final String SIN_CORREO = "SIN_CORREO";

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final String VERDE_ROOMIX = "#397d54";

    private final ReservaRepository reservaRepository;
    private final AbonoService abonoService;
    private final PoliticaReservaService politicaService;
    private final ConfiguracionHotelService configuracion;
    private final CorreoService correo;
    private final TransactionTemplate nuevaTransaccion;
    private final String urlPublica;
    private final ExecutorService envios = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "correo-reservas");
        t.setDaemon(true);
        return t;
    });

    public CorreoReservaService(ReservaRepository reservaRepository, AbonoService abonoService, PoliticaReservaService politicaService,
                                ConfiguracionHotelService configuracion, CorreoService correo, PlatformTransactionManager transacciones,
                                @Value("${app.url-publica:}") String urlPublica) {
        this.reservaRepository = reservaRepository;
        this.abonoService = abonoService;
        this.politicaService = politicaService;
        this.configuracion = configuracion;
        this.correo = correo;
        this.nuevaTransaccion = new TransactionTemplate(transacciones);
        this.nuevaTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.urlPublica = StringUtils.hasText(urlPublica) ? urlPublica.replaceAll("/+$", "") : null;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alConfirmar(ReservaConfirmadaEvent evento) {
        automatico(evento.reservaId(), TipoCorreo.CONFIRMACION);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alModificar(ReservaModificadaEvent evento) {
        automatico(evento.reservaId(), TipoCorreo.MODIFICACION);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alCancelar(ReservaCanceladaEvent evento) {
        automatico(evento.reservaId(), TipoCorreo.CANCELACION);
    }

    /**
     * Reenvío manual desde el detalle de la reserva (aunque el envío automático esté apagado): confirmación o, si la
     * reserva se canceló, el aviso de cancelación. Se envía en el momento y devuelve el resultado.
     */
    @Transactional(readOnly = true)
    public String reenviar(Long reservaId) {
        if (!correo.habilitado()) {
            throw new ReservaException(ReservaErrorCode.CORREO_NO_CONFIGURADO);
        }
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaException(ReservaErrorCode.RESERVA_NO_ENCONTRADA, "Reserva no encontrada: " + reservaId));
        if (!tieneCorreo(r)) {
            throw new ReservaException(ReservaErrorCode.HUESPED_SIN_CORREO);
        }
        TipoCorreo tipo = r.getEstadoReserva() == EstadoReserva.CANCELADA ? TipoCorreo.CANCELACION : TipoCorreo.CONFIRMACION;
        Mensaje m = mensaje(r, tipo);
        boolean ok = correo.enviarHtml(m.destino(), m.nombre(), m.remitente(), m.asunto(), m.html(), m.texto());
        String estado = ok ? ENVIADO : FALLIDO;
        registrar(reservaId, estado, tipo);
        return estado;
    }

    private void automatico(Long reservaId, TipoCorreo tipo) {
        try {
            if (!configuracion.correoConfirmacionReserva() || !correo.habilitado()) {
                return;
            }
            Reserva r = reservaRepository.findById(reservaId).orElse(null);
            if (r == null) {
                return;
            }
            if (!tieneCorreo(r)) {
                registrar(reservaId, SIN_CORREO, tipo);
                return;
            }
            Mensaje m = mensaje(r, tipo);
            envios.submit(() -> {
                boolean ok = correo.enviarHtml(m.destino(), m.nombre(), m.remitente(), m.asunto(), m.html(), m.texto());
                registrar(reservaId, ok ? ENVIADO : FALLIDO, tipo);
            });
        } catch (Exception e) {
            log.warn("No se pudo preparar el correo ({}) de la reserva {}: {}", tipo, reservaId, e.getMessage());
            registrar(reservaId, FALLIDO, tipo);
        }
    }

    /** Guarda el resultado sin tocar la versión de la reserva (no compite con quien la esté editando). */
    private void registrar(Long reservaId, String estado, TipoCorreo tipo) {
        try {
            nuevaTransaccion.executeWithoutResult(s -> reservaRepository.registrarCorreo(reservaId, estado, tipo.name(), LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("No se pudo registrar el estado del correo de la reserva {}: {}", reservaId, e.getMessage());
        }
    }

    private static boolean tieneCorreo(Reserva r) {
        return r.getHuesped() != null && StringUtils.hasText(r.getHuesped().getEmail())
                && !Boolean.TRUE.equals(r.getHuesped().getAnonimizado())
                && !r.getHuesped().getEmail().endsWith(".invalid");
    }

    private record Mensaje(String destino, String nombre, String remitente, String asunto, String html, String texto) {
    }

    private Mensaje mensaje(Reserva r, TipoCorreo tipo) {
        ConfiguracionHotel hotel = configuracion.entidad();
        ResumenAbonos abonos = abonoService.resumen(r.getId());
        BigDecimal penalidad = politicaService.vigente().getPenalidadCancelacionPct();
        String asunto = switch (tipo) {
            case CONFIRMACION -> "Reserva confirmada #";
            case MODIFICACION -> "Reserva modificada #";
            case CANCELACION -> "Reserva cancelada #";
        } + r.getId() + " — " + hotel.getNombre();
        return new Mensaje(r.getHuesped().getEmail().trim(), r.getHuesped().nombreCompleto(), hotel.getNombre(), asunto,
                html(r, hotel, abonos, penalidad, tipo), texto(r, hotel, abonos, penalidad, tipo));
    }

    private String html(Reserva r, ConfiguracionHotel h, ResumenAbonos a, BigDecimal penalidad, TipoCorreo tipo) {
        String color = colorSeguro(h.getColorPrimario());
        StringBuilder filas = new StringBuilder();
        fila(filas, "Habitación", r.getHabitacion().getNumero() + " (" + r.getHabitacion().getTipoHabitacion().name().toLowerCase() + ")");
        fila(filas, "Llegada", FECHA.format(r.getFechaEntrada()));
        fila(filas, "Salida", FECHA.format(r.getFechaSalida()));
        fila(filas, "Noches", String.valueOf(r.getCantidadNoches()));
        fila(filas, "Personas", String.valueOf(r.personas()));
        if (tipo == TipoCorreo.CANCELACION) {
            if (r.getValorDevuelto() != null && r.getValorDevuelto().signum() > 0) {
                fila(filas, "Valor que se le devuelve", pesos(r.getValorDevuelto()));
            }
            if (r.getValorPenalidad() != null && r.getValorPenalidad().signum() > 0) {
                fila(filas, "Penalidad retenida", pesos(r.getValorPenalidad()));
            }
        } else {
            fila(filas, "Total de la estadía", pesos(a.totalReserva()));
            fila(filas, "Abonado", pesos(a.totalAbonado()));
            fila(filas, "Saldo por pagar", pesos(a.saldo()));
        }
        String intro = switch (tipo) {
            case CONFIRMACION -> "Tu reserva está <strong>confirmada</strong>. Estos son los datos:";
            case MODIFICACION -> "Actualizamos tu reserva. Estos son los <strong>datos nuevos</strong>:";
            case CANCELACION -> "Tu reserva fue <strong>cancelada</strong>.";
        };
        String cierre = tipo == TipoCorreo.CANCELACION ? "Esperamos recibirte en otra ocasión." : "Te esperamos.";
        String nota = tipo != TipoCorreo.CANCELACION && penalidad.signum() > 0
                ? "<p style=\"font-size:13px;color:#5b6660\">Si cancelas, el hotel retiene el " + penalidad.stripTrailingZeros().toPlainString()
                + " % de lo abonado y devuelve el resto.</p>" : "";
        String logo = urlPublica != null && h.tieneLogo()
                ? "<img src=\"" + urlPublica + "/api/configuracion/logo?v=" + version(h) + "\" alt=\"\" style=\"" + estiloLogo(h) + "\">"
                : "";
        String contacto = String.join(" · ", java.util.stream.Stream.of(h.getDireccion(), h.getCiudad(), h.getTelefono(), h.getEmail())
                .filter(StringUtils::hasText).map(HtmlUtils::htmlEscape).toList());
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2a24">
                  <div style="border-top:6px solid %s;padding:18px 0 4px">%s
                    <h2 style="color:%s;margin:0 0 4px">%s</h2>
                    <p style="margin:0 0 16px;color:#5b6660">%s #%d</p>
                  </div>
                  <p>Hola, %s:</p>
                  <p>%s</p>
                  <table style="border-collapse:collapse;width:100%%">%s</table>
                  %s
                  <p style="margin-top:20px">%s</p>
                  <p style="font-size:12px;color:#5b6660;border-top:1px solid #e3e8e5;padding-top:10px">%s</p>
                </div>
                """.formatted(color, logo, color, HtmlUtils.htmlEscape(h.getNombre()), titulo(tipo), r.getId(),
                HtmlUtils.htmlEscape(r.getHuesped().getNombre()), intro, filas, nota, cierre, contacto);
    }

    private String texto(Reserva r, ConfiguracionHotel h, ResumenAbonos a, BigDecimal penalidad, TipoCorreo tipo) {
        StringBuilder t = new StringBuilder(h.getNombre()).append("\n").append(titulo(tipo)).append(" #").append(r.getId())
                .append("\n\nHola, ").append(r.getHuesped().getNombre()).append(": ")
                .append(switch (tipo) {
                    case CONFIRMACION -> "tu reserva está confirmada.";
                    case MODIFICACION -> "actualizamos tu reserva; estos son los datos nuevos.";
                    case CANCELACION -> "tu reserva fue cancelada.";
                })
                .append("\n\nHabitación: ").append(r.getHabitacion().getNumero())
                .append("\nLlegada: ").append(FECHA.format(r.getFechaEntrada()))
                .append("\nSalida: ").append(FECHA.format(r.getFechaSalida()))
                .append("\nNoches: ").append(r.getCantidadNoches())
                .append("\nPersonas: ").append(r.personas());
        if (tipo == TipoCorreo.CANCELACION) {
            if (r.getValorDevuelto() != null && r.getValorDevuelto().signum() > 0) {
                t.append("\nValor que se le devuelve: ").append(pesos(r.getValorDevuelto()));
            }
            t.append("\n\nEsperamos recibirte en otra ocasión.");
        } else {
            t.append("\nTotal: ").append(pesos(a.totalReserva())).append("\nAbonado: ").append(pesos(a.totalAbonado()))
                    .append("\nSaldo: ").append(pesos(a.saldo()));
            if (penalidad.signum() > 0) {
                t.append("\n\nSi cancelas, el hotel retiene el ").append(penalidad.stripTrailingZeros().toPlainString()).append(" % de lo abonado.");
            }
            t.append("\n\nTe esperamos.");
        }
        return t.toString();
    }

    private static String titulo(TipoCorreo tipo) {
        return switch (tipo) {
            case CONFIRMACION -> "Confirmación de reserva";
            case MODIFICACION -> "Cambio en tu reserva";
            case CANCELACION -> "Cancelación de reserva";
        };
    }

    /** El logo en el correo respeta la forma elegida (círculo o esquinas redondeadas, con fondo blanco). */
    private static String estiloLogo(ConfiguracionHotel h) {
        return switch (h.forma()) {
            case ORIGINAL -> "max-height:56px;max-width:160px;display:block;margin:0 0 10px";
            case ICONO -> "width:56px;height:56px;object-fit:contain;background:#ffffff;padding:4px;border-radius:12px;display:block;margin:0 0 10px";
            case AVATAR -> "width:56px;height:56px;object-fit:contain;background:#ffffff;padding:4px;border-radius:50%;display:block;margin:0 0 10px";
        };
    }

    /** Solo colores #RRGGBB (se escriben dentro del HTML del correo); si no, el verde de Roomix. */
    private static String colorSeguro(String color) {
        return color != null && color.matches("^#[0-9a-fA-F]{6}$") ? color : VERDE_ROOMIX;
    }

    private static long version(ConfiguracionHotel h) {
        return h.getFechaHoraActualizacion() == null ? 0
                : h.getFechaHoraActualizacion().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
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
