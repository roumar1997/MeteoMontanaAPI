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
}
