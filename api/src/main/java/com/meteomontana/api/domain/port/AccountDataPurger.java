package com.meteomontana.api.domain.port;

/**
 * Borrado de TODOS los datos locales de un usuario (RGPD "derecho al olvido").
 *
 * El caso de uso expresa la INTENCIÓN ("purga los datos de este uid"); qué
 * tablas existen y en qué orden se vacían es un detalle de infraestructura.
 * Lo que NO se borra aquí (contenido comunitario ya publicado, Firestore,
 * Storage, Firebase Auth) lo decide el caso de uso.
 */
public interface AccountDataPurger {

    /** Ruta de la foto de perfil, ANTES de purgar (para borrarla del Storage). */
    String photoPathOf(String uid);

    /** Vacía favoritas, follows, diario, notificaciones, notas, propuestas,
     *  alertas y la ficha del usuario. */
    void purgeAllDataOf(String uid);
}
