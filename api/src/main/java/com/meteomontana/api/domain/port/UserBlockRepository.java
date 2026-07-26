package com.meteomontana.api.domain.port;

import java.util.Set;

/**
 * Bloqueos entre usuarios: el bloqueador deja de ver contenido del bloqueado.
 * Puerto de LECTURA para las reglas de acceso (feed, comentarios); el alta y
 * baja de bloqueos vive en la capa de moderación.
 */
public interface UserBlockRepository {

    /** Uids que {@code blockerUid} ha bloqueado (para filtrar páginas/hilos). */
    Set<String> blockedUidsOf(String blockerUid);

    /** ¿{@code blockerUid} ha bloqueado a {@code blockedUid}? */
    boolean isBlocked(String blockerUid, String blockedUid);
}
