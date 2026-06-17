package com.meteomontana.api.infrastructure.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Adaptador de {@link com.meteomontana.api.domain.port.PushSender} sobre el FCM
 * Admin SDK. La capa de aplicación depende del puerto, no de esta clase.
 */
@Service
public class FcmService implements com.meteomontana.api.domain.port.PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    /** Envía push a un dispositivo concreto. Si el token es inválido, lo logueamos. */
    @Override
    public boolean sendToToken(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isBlank()) return false;
        try {
            Message.Builder msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null) msg.putAllData(data);

            String id = FirebaseMessaging.getInstance().send(msg.build());
            log.debug("FCM sent: {}", id);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed (token={}…): {}", truncate(token), e.getMessage());
            return false;
        }
    }

    /** No volcamos el token FCM entero a los logs. */
    private static String truncate(String token) {
        return token.length() > 12 ? token.substring(0, 12) : token;
    }

    /**
     * Envía push SOLO con data (sin bloque notification). Así onMessageReceived
     * de la app se ejecuta siempre — también con la app en background — y puede
     * construir la notificación nativa con extras como el avatar del seguidor.
     * El data map debe incluir "title" y "body".
     */
    @Override
    public boolean sendDataToToken(String token, Map<String, String> data) {
        if (token == null || token.isBlank()) return false;
        try {
            String id = FirebaseMessaging.getInstance().send(
                    Message.builder().setToken(token).putAllData(data).build());
            log.debug("FCM data sent: {}", id);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM data send failed (token={}…): {}", truncate(token), e.getMessage());
            return false;
        }
    }

    /** Envía a todos los usuarios con token (broadcast simple, iterativo). */
    @Override
    public int sendToTokens(Iterable<String> tokens, String title, String body, Map<String, String> data) {
        int ok = 0;
        for (String token : tokens) {
            if (sendToToken(token, title, body, data)) ok++;
        }
        return ok;
    }
}
