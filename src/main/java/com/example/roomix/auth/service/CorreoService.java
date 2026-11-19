package com.example.roomix.auth.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de correos (opcional). Con BREVO_API_KEY usa la API HTTPS de Brevo, que funciona en plataformas que bloquean el
 * SMTP (como el plan Hobby de Railway); si no, usa SMTP (SPRING_MAIL_*). En ambos casos MAIL_FROM es el remitente, que
 * debe estar verificado en el proveedor. Sin configuración, los correos no se envían y se deja constancia en el log.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorreoService {

    private static final URI BREVO = URI.create("https://api.brevo.com/v3/smtp/email");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${app.mail.from:}")
    private String remitente;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    public boolean habilitado() {
        return StringUtils.hasText(remitente)
                && (StringUtils.hasText(brevoApiKey) || (StringUtils.hasText(host) && mailSender.getIfAvailable() != null));
    }

    /** Correo de texto (recuperación de contraseña). Devuelve false si no se pudo enviar. */
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        return enviarHtml(destinatario, null, null, asunto, null, cuerpo);
    }

    /** Correo con versión HTML y de texto; nombreRemitente es el nombre visible (por ejemplo, el del hotel). */
    public boolean enviarHtml(String destinatario, String nombreDestinatario, String nombreRemitente, String asunto,
                              String html, String texto) {
        if (!habilitado()) {
            log.info("Correo no configurado: no se envió '{}' a {}", asunto, destinatario);
            return false;
        }
        try {
            if (StringUtils.hasText(brevoApiKey)) {
                return enviarBrevo(destinatario, nombreDestinatario, nombreRemitente, asunto, html, texto);
            }
            MimeMessage mensaje = mailSender.getObject().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, html != null, StandardCharsets.UTF_8.name());
            if (StringUtils.hasText(nombreRemitente)) {
                helper.setFrom(remitente, nombreRemitente);
            } else {
                helper.setFrom(remitente);
            }
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            if (html != null) {
                helper.setText(texto != null ? texto : "", html);
            } else {
                helper.setText(texto != null ? texto : "");
            }
            mailSender.getObject().send(mensaje);
            return true;
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo a {}: {}", destinatario, ex.getMessage());
            return false;
        }
    }

    private boolean enviarBrevo(String destinatario, String nombreDestinatario, String nombreRemitente, String asunto,
                                String html, String texto) throws Exception {
        Map<String, Object> remitenteJson = new LinkedHashMap<>();
        remitenteJson.put("email", remitente);
        if (StringUtils.hasText(nombreRemitente)) {
            remitenteJson.put("name", nombreRemitente);
        }
        Map<String, Object> para = new LinkedHashMap<>();
        para.put("email", destinatario);
        if (StringUtils.hasText(nombreDestinatario)) {
            para.put("name", nombreDestinatario);
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sender", remitenteJson);
        cuerpo.put("to", List.of(para));
        cuerpo.put("subject", asunto);
        if (html != null) {
            cuerpo.put("htmlContent", html);
        }
        if (texto != null) {
            cuerpo.put("textContent", texto);
        }
        HttpRequest peticion = HttpRequest.newBuilder(BREVO)
                .timeout(Duration.ofSeconds(20))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(aJson(cuerpo), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() / 100 != 2) {
            log.error("Brevo rechazó el correo a {} (HTTP {}): {}", destinatario, respuesta.statusCode(), respuesta.body());
            return false;
        }
        return true;
    }

    /** JSON mínimo para la API de Brevo (cadenas, listas y mapas anidados). */
    @SuppressWarnings("unchecked")
    private static String aJson(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            for (var e : m.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append(aJson(String.valueOf(e.getKey()))).append(':').append(aJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");
            for (Object o : l) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                sb.append(aJson(o));
            }
            return sb.append(']').toString();
        }
        String s = String.valueOf(v);
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
