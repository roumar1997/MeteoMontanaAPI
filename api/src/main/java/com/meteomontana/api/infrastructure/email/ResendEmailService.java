package com.meteomontana.api.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de envío de emails transaccionales vía Resend (https://resend.com).
 * Configurar `RESEND_API_KEY` y `RESEND_FROM` como variables de entorno o
 * properties. Si la API key no está configurada, el método retorna false y
 * loguea (no rompe la app).
 */
@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${RESEND_API_KEY:}")
    private String apiKey;

    @Value("${RESEND_FROM:MeteoMontana <noreply@climbingteams.com>}")
    private String from;

    private final RestTemplate http = new RestTemplate();

    /** Devuelve true si el email se envió a Resend. */
    public boolean send(String to, String subject, String htmlBody) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Resend desactivado (sin API key). Skipping email a {}", to);
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("from", from);
            body.put("to", new String[]{to});
            body.put("subject", subject);
            body.put("html", htmlBody);

            http.postForObject(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Email Resend enviado a {} (asunto: {})", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("Fallo Resend a {}: {}", to, e.getMessage());
            return false;
        }
    }
}
