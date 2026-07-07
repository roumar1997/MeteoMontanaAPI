package com.meteomontana.api.infrastructure.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserDeviceRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserDeviceJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de {@link com.meteomontana.api.domain.port.PushSender} sobre el FCM
 * Admin SDK. La capa de aplicación depende del puerto, no de esta clase.
 */
@Service
public class FcmService implements com.meteomontana.api.domain.port.PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final SpringDataUserDeviceRepository devices;

    public FcmService(SpringDataUserDeviceRepository devices) {
        this.devices = devices;
    }

    // Debe coincidir con el canal creado en la app (PushService.CHANNEL_ID) y el
    // icono/color de la marca, para cuando es el SISTEMA quien pinta la notificación
    // (app en background o cerrada → onMessageReceived NO corre).
    private static final String ANDROID_CHANNEL_ID = "meteomontana_general";
    private static final String ANDROID_ICON = "ic_notification";
    private static final String ANDROID_COLOR = "#C0532B"; // Terra

    /**
     * Config Android con PRIORIDAD ALTA. Sin esto, un push a una app cerrada en
     * móviles con gestión agresiva de batería (Xiaomi/MIUI, Doze) se retrasa o se
     * descarta → la notificación "no llega" hasta abrir la app. Incluye canal,
     * icono y color para que el sistema pinte la notificación con la marca.
     */
    private static AndroidConfig highPriorityAndroid() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setChannelId(ANDROID_CHANNEL_ID)
                        .setIcon(ANDROID_ICON)
                        .setColor(ANDROID_COLOR)
                        .build())
                .build();
    }

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
                            .build())
                    .setAndroidConfig(highPriorityAndroid());
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
     * Push para las notificaciones sociales (follows, alertas). El data map incluye
     * "title"/"body" + targetType/targetId (deep-link) + avatarUrl opcional.
     *
     * Lleva bloque `notification` Y data, con prioridad ALTA:
     *  - App en PRIMER PLANO → onMessageReceived construye la notificación nativa
     *    con el avatar del seguidor (largeIcon circular).
     *  - App en BACKGROUND o CERRADA → la pinta el sistema (fiable aunque la app
     *    esté muerta; antes era solo-data y Xiaomi no la despertaba → no llegaba).
     *    Al tocarla, el `data` llega como extras del intent → deep-link igual.
     */
    @Override
    public boolean sendDataToToken(String token, Map<String, String> data) {
        if (token == null || token.isBlank()) return false;
        try {
            String title = data != null ? data.getOrDefault("title", "Cumbre") : "Cumbre";
            String body  = data != null ? data.getOrDefault("body", "") : "";
            Message.Builder msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(highPriorityAndroid());
            if (data != null) msg.putAllData(data);

            String id = FirebaseMessaging.getInstance().send(msg.build());
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

    /**
     * Envía a TODOS los dispositivos del usuario (tabla user_devices). Antes solo
     * existía users.fcm_token → iniciar sesión en un segundo móvil machacaba el
     * token del primero y este dejaba de recibir push. Los tokens que FCM rechaza
     * se borran de la tabla (dispositivo desinstalado/caducado).
     */
    @Override
    public int sendToUser(String uid, String title, String body, Map<String, String> data) {
        int ok = 0;
        for (UserDeviceJpaEntity device : devices.findByUid(uid)) {
            if (sendToToken(device.getToken(), title, body, data)) ok++;
            else devices.deleteById(device.getToken());
        }
        return ok;
    }

    /** Variante data (ver {@link #sendDataToToken}) a todos los dispositivos del usuario. */
    @Override
    public int sendDataToUser(String uid, Map<String, String> data) {
        int ok = 0;
        for (UserDeviceJpaEntity device : devices.findByUid(uid)) {
            if (sendDataToToken(device.getToken(), data)) ok++;
            else devices.deleteById(device.getToken());
        }
        return ok;
    }

    // Límite de FCM para un envío multicast: 500 tokens por llamada.
    private static final int MULTICAST_BATCH = 500;

    /** {@inheritDoc} Corre en el pool "pushExecutor" (fuera del hilo de la request). */
    @Async("pushExecutor")
    @Override
    public void sendDataToUserAsync(String uid, Map<String, String> data) {
        List<String> tokens = tokensOf(devices.findByUid(uid));
        if (!tokens.isEmpty()) multicast(tokens, data);
    }

    /** {@inheritDoc} Corre en el pool "pushExecutor"; agrupa en tandas de 500. */
    @Async("pushExecutor")
    @Override
    public void sendDataToUsersAsync(Collection<String> uids, Map<String, String> data) {
        List<UserDeviceJpaEntity> all = new ArrayList<>();
        for (String uid : uids) {
            if (uid != null && !uid.isBlank()) all.addAll(devices.findByUid(uid));
        }
        List<String> tokens = tokensOf(all);
        for (int i = 0; i < tokens.size(); i += MULTICAST_BATCH) {
            multicast(tokens.subList(i, Math.min(i + MULTICAST_BATCH, tokens.size())), data);
        }
    }

    private static List<String> tokensOf(List<UserDeviceJpaEntity> deviceList) {
        List<String> tokens = new ArrayList<>(deviceList.size());
        for (UserDeviceJpaEntity d : deviceList) {
            if (d.getToken() != null && !d.getToken().isBlank()) tokens.add(d.getToken());
        }
        return tokens;
    }

    /**
     * Envía el MISMO push a muchos dispositivos en UNA sola llamada HTTP a FCM
     * ({@code sendEachForMulticast}), en vez de una llamada por dispositivo. El
     * título/cuerpo se leen del propio {@code data} (donde ya viajan "title"/"body").
     * Los tokens que FCM marca como caducados/inválidos se borran de la tabla.
     */
    private void multicast(List<String> tokens, Map<String, String> data) {
        if (tokens.isEmpty()) return;
        String title = data != null ? data.getOrDefault("title", "Cumbre") : "Cumbre";
        String body  = data != null ? data.getOrDefault("body", "") : "";
        MulticastMessage.Builder msg = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .setAndroidConfig(highPriorityAndroid());
        if (data != null) msg.putAllData(data);
        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(msg.build());
            List<SendResponse> responses = resp.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse r = responses.get(i);
                if (r.isSuccessful()) continue;
                MessagingErrorCode code = r.getException() != null
                        ? r.getException().getMessagingErrorCode() : null;
                // Token muerto (app desinstalada / token caducado) → limpiar.
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    try { devices.deleteById(tokens.get(i)); } catch (Exception ignored) {}
                }
            }
            log.debug("FCM multicast: {}/{} ok", resp.getSuccessCount(), responses.size());
        } catch (FirebaseMessagingException e) {
            log.warn("FCM multicast failed ({} tokens): {}", tokens.size(), e.getMessage());
        }
    }
}
