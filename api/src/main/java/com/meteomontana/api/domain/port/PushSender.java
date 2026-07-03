package com.meteomontana.api.domain.port;

import java.util.Map;

/**
 * Puerto de envío de notificaciones push. La capa de aplicación depende de esta
 * abstracción (no de FCM directamente), respetando la regla de dependencias
 * hexagonal. El adaptador es {@code infrastructure.push.FcmService}.
 */
public interface PushSender {

    /** Push a un dispositivo (con bloque notification). false si el token es inválido. */
    boolean sendToToken(String token, String title, String body, Map<String, String> data);

    /** Push solo-data (sin notification) → la app la construye en onMessageReceived. */
    boolean sendDataToToken(String token, Map<String, String> data);

    /** Broadcast simple a varios tokens; devuelve cuántos se enviaron. */
    int sendToTokens(Iterable<String> tokens, String title, String body, Map<String, String> data);

    /** Push a TODOS los dispositivos del usuario (tabla user_devices). Devuelve cuántos llegaron. */
    int sendToUser(String uid, String title, String body, Map<String, String> data);

    /** Push solo-data a todos los dispositivos del usuario. Devuelve cuántos llegaron. */
    int sendDataToUser(String uid, Map<String, String> data);
}
