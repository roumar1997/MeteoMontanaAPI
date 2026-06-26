package com.meteomontana.api.domain.port;

/**
 * Acceso de solo lectura al chat 1-a-1, que vive en Firestore. El backend lo
 * consulta para decidir, p.ej., si dos usuarios ya tienen una conversación
 * abierta (en cuyo caso ambos pueden seguir hablando aunque no haya relación
 * de seguimiento y el receptor sea privado).
 */
public interface ChatRepository {

    /**
     * True si ya existe una conversación entre los dos usuarios (en cualquier
     * sentido). El id de conversación se calcula con los dos uids ordenados
     * alfabéticamente y unidos por "_", igual que en las apps Android e iOS.
     */
    boolean conversationExists(String uidA, String uidB);

    /**
     * Crea el documento de conversación entre los dos usuarios si aún no existe
     * (idempotente). Solo el backend crea conversaciones; las reglas de Firestore
     * impiden a los clientes crearlas. Una vez existe, ambos participantes pueden
     * escribir mensajes.
     */
    void ensureConversation(String uidA, String uidB);

    /**
     * Crea un GRUPO de chat (varias personas) en Firestore y devuelve su convId.
     * participants = creador + memberUids (sin duplicados). El doc lleva
     * {@code isGroup=true} y {@code name}. Solo el backend crea conversaciones.
     */
    String createGroup(String creatorUid, String name, java.util.List<String> memberUids);

    /**
     * Lista de uids participantes de una conversación (para notificar a todos los
     * miembros de un grupo). Vacía si no existe o ante error.
     */
    java.util.List<String> participantsOf(String convId);

    /**
     * Actualiza la lista de participants de un grupo (join/kick).
     * Idempotente — si el uid ya está / ya no está, no falla.
     */
    void updateParticipants(String convId, java.util.List<String> participants);

    /**
     * Borra toda la conversación (doc + subcolección messages). Usado cuando
     * caduca una quedada. Best-effort: no propaga errores.
     */
    void deleteConversation(String convId);

    /**
     * Borra los datos de chat del usuario al eliminar su cuenta (RGPD): elimina
     * las conversaciones 1-a-1 en las que participa (y sus mensajes) y lo quita
     * de los grupos (borrando el grupo si se queda sin participantes).
     * Best-effort: no propaga errores.
     */
    void deleteUserData(String uid);
}
