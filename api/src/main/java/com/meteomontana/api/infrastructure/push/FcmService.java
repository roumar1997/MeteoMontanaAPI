package com.meteomontana.api.infrastructure.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserDeviceRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserDeviceJpaEntity;
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
}
