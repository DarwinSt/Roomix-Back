package com.example.roomix.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Envío de correos. Es opcional: solo funciona si se configura un servidor SMTP
 * (SPRING_MAIL_HOST, SPRING_MAIL_PORT, SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD y MAIL_FROM).
 * Sin SMTP, el administrador puede generar contraseñas temporales desde el módulo de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${app.mail.from:}")
    private String remitente;

    public boolean habilitado() {
        return StringUtils.hasText(host) && StringUtils.hasText(remitente) && mailSender.getIfAvailable() != null;
    }

    /** Envía el correo si hay SMTP configurado. Devuelve false si no se pudo enviar. */
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if (!habilitado()) {
            log.info("Correo no configurado: no se envió '{}' a {}", asunto, destinatario);
            return false;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.getObject().send(mensaje);
            return true;
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo a {}: {}", destinatario, ex.getMessage());
            return false;
        }
    }
}
