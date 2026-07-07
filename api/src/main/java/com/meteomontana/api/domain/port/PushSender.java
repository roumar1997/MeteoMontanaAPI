package com.meteomontana.api.domain.port;

import java.util.Collection;
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

    /**
     * Fire-and-forget: envía el push a todos los dispositivos del usuario EN
     * SEGUNDO PLANO (no bloquea el hilo de la request). El adaptador agrupa los
     * tokens en una sola llamada a FCM (multicast). Pensado para el path de
     * follows/mensajes 1-a-1, donde la respuesta al usuario no debe esperar al push.
     */
    void sendDataToUserAsync(String uid, Map<String, String> data);

    /**
     * Fire-and-forget en LOTE: envía el mismo push a los dispositivos de VARIOS
     * usuarios (chat de grupo / quedadas) en segundo plano, agrupando en tandas
     * de hasta 500 tokens por llamada a FCM. Antes se hacía un envío bloqueante
     * por participante × dispositivo dentro de la request → cuello en quedadas
     * concurridas. Ahora es una (o pocas) llamada(s) fuera del hilo de la request.
     */
    void sendDataToUsersAsync(Collection<String> uids, Map<String, String> data);
}
