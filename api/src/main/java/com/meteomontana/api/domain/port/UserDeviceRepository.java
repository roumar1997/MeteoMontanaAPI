package com.meteomontana.api.domain.port;

/**
 * Dispositivos (tokens FCM) por usuario: cada app registra SU token al
 * arrancar, así el push llega a todos los aparatos del usuario. Puerto de
 * dominio — la tabla y su mapeo viven en infraestructura.
 */
public interface UserDeviceRepository {
    /** Registra el token para [uid]; si ya existía con otro usuario, lo reasigna. */
    void registerDevice(String token, String uid);

    /** Todos los tokens registrados (push masivo del admin). */
    java.util.List<String> allTokens();
}
