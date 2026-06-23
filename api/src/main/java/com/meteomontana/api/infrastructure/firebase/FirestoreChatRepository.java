package com.meteomontana.api.infrastructure.firebase;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.meteomontana.api.domain.port.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Adaptador Firestore del puerto {@link ChatRepository}. El chat 1-a-1 vive en
 * la colección {@code conversations/{convId}}, donde {@code convId} son los dos
 * uids ordenados alfabéticamente y unidos por "_" (mismo cálculo que las apps).
 */
@Repository
public class FirestoreChatRepository implements ChatRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestoreChatRepository.class);

    private final Firestore firestore;

    public FirestoreChatRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public boolean conversationExists(String uidA, String uidB) {
        if (uidA == null || uidB == null || uidA.isBlank() || uidB.isBlank()) return false;
        String convId = conversationId(uidA, uidB);
        try {
            return firestore.collection("conversations")
                    .document(convId)
                    .get()
                    .get()            // bloquea hasta resolver el ApiFuture
                    .exists();
        } catch (Exception e) {
            // Ante cualquier fallo de red/permiso, no confirmamos la conversación.
            // El resto de condiciones (receptor público / follow) siguen aplicando.
            log.warn("No se pudo comprobar la conversación {}: {}", convId, e.toString());
            return false;
        }
    }

    @Override
    public void ensureConversation(String uidA, String uidB) {
        if (uidA == null || uidB == null || uidA.isBlank() || uidB.isBlank()) return;
        String convId = conversationId(uidA, uidB);
        List<String> participants = Arrays.asList(uidA, uidB);
        participants.sort(null);
        try {
            // merge: si ya existe no pisa lastMessage/unread/cleared; si no, lo crea
            // con los participantes (que es lo que las reglas y la lista necesitan).
            firestore.collection("conversations")
                    .document(convId)
                    .set(Map.of("participants", participants), SetOptions.merge())
                    .get();
        } catch (Exception e) {
            // A diferencia de conversationExists (check suave), aquí sí propagamos:
            // si no se crea el doc, el cliente no podrá escribir el primer mensaje.
            log.warn("No se pudo crear la conversación {}: {}", convId, e.toString());
            throw new RuntimeException("No se pudo crear la conversación", e);
        }
    }

    @Override
    public String createGroup(String creatorUid, String name, List<String> memberUids) {
        // participants = creador + miembros (sin duplicados, conservando orden).
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(creatorUid);
        if (memberUids != null) {
            for (String m : memberUids) if (m != null && !m.isBlank()) set.add(m);
        }
        List<String> participants = new ArrayList<>(set);
        try {
            var doc = firestore.collection("conversations").document();   // id aleatorio
            // OJO: la query de las apps ordena por "lastAt"; en Firestore un
            // orderBy EXCLUYE los documentos que no tienen ese campo. Sin lastAt
            // el grupo recién creado no aparecería en observeMyConversations()
            // (→ "1 miembros / ya no eres miembro" y no se puede escribir). Por eso
            // sembramos lastAt al crear el grupo.
            doc.set(Map.of(
                    "participants", participants,
                    "isGroup", true,
                    "name", name == null ? "Grupo" : name,
                    "createdBy", creatorUid,
                    "lastAt", com.google.cloud.Timestamp.now(),
                    "lastMessage", "Grupo creado",
                    "lastFromUid", creatorUid
            )).get();
            return doc.getId();
        } catch (Exception e) {
            log.warn("No se pudo crear el grupo '{}': {}", name, e.toString());
            throw new RuntimeException("No se pudo crear el grupo", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> participantsOf(String convId) {
        if (convId == null || convId.isBlank()) return List.of();
        try {
            var snap = firestore.collection("conversations").document(convId).get().get();
            Object p = snap.get("participants");
            if (p instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) if (o instanceof String s) out.add(s);
                return out;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("No se pudieron leer los participantes de {}: {}", convId, e.toString());
            return List.of();
        }
    }

    private static String conversationId(String uidA, String uidB) {
        String[] ids = {uidA, uidB};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }
}
