package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.push.FcmService;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint que el cliente Android llama después de escribir un mensaje
 * en Firestore, para que el backend dispare la push notification.
 *
 * Body: {"toUid":"<uid_receptor>", "preview":"<texto_corto>"}
 */
@RestController
@RequestMapping("/api/chat")
public class ChatPushController {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ChatRepository chatRepository;
    private final FcmService fcmService;

    public ChatPushController(UserRepository userRepository,
                              FollowRepository followRepository,
                              ChatRepository chatRepository,
                              FcmService fcmService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.chatRepository = chatRepository;
        this.fcmService = fcmService;
    }

    public record NotifyRequest(String toUid, String preview) {}

    public record StartRequest(String toUid) {}

    /**
     * Inicia (o asegura) una conversación 1-a-1. Es la puerta de autorización del
     * chat: solo el backend crea conversaciones en Firestore. Se permite si el
     * receptor es público, o si hay relación de seguimiento aceptada en cualquier
     * sentido. Una vez creada la conversación, las reglas de Firestore dejan a
     * ambos participantes escribir mensajes (incluida la respuesta de un usuario
     * privado al que le escribieron primero).
     */
    @PostMapping("/start")
    public ResponseEntity<Void> start(
            @AuthenticationPrincipal FirebaseUser me,
            @RequestBody StartRequest req) {
        if (req == null || req.toUid() == null || req.toUid().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (me.uid().equals(req.toUid())) return ResponseEntity.badRequest().build();

        User to = userRepository.findByUid(req.toUid()).orElse(null);
        if (to == null) return ResponseEntity.notFound().build();

        boolean allowed = to.isPublic()
                || followRepository.isFollowing(me.uid(), req.toUid())
                || followRepository.isFollowing(req.toUid(), me.uid())
                || chatRepository.conversationExists(me.uid(), req.toUid());
        if (!allowed) return ResponseEntity.status(403).build();

        chatRepository.ensureConversation(me.uid(), req.toUid());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(
            @AuthenticationPrincipal FirebaseUser sender,
            @RequestBody NotifyRequest req) {
        if (req == null || req.toUid() == null || req.toUid().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // No mandes push a ti mismo
        if (sender.uid().equals(req.toUid())) return ResponseEntity.ok().build();

        User to = userRepository.findByUid(req.toUid()).orElse(null);
        if (to == null || to.getFcmToken() == null || to.getFcmToken().isBlank()) {
            return ResponseEntity.ok().build();   // sin token, simplemente no se manda
        }

        // Modelo de privacidad del chat: se permite avisar al receptor si
        //  - el receptor es PÚBLICO (cualquiera puede escribirle), o
        //  - el emisor SIGUE (aceptado) al receptor —el privado aceptó su solicitud—, o
        //  - el receptor sigue (aceptado) al emisor, o
        //  - YA existe una conversación abierta entre ambos (entonces ambos pueden
        //    seguir hablando aunque no haya follow y el receptor sea privado).
        // Si no se cumple ninguna, se ignora silenciosamente (no se filtra si el
        // otro existe o tiene token). El mensaje en sí vive en Firestore aparte.
        boolean allowed = to.isPublic()
                || followRepository.isFollowing(sender.uid(), req.toUid())
                || followRepository.isFollowing(req.toUid(), sender.uid())
                || chatRepository.conversationExists(sender.uid(), req.toUid());
        if (!allowed) return ResponseEntity.ok().build();
        User from = userRepository.findByUid(sender.uid()).orElse(null);
        String fromName = (from != null && from.getDisplayName() != null)
                ? from.getDisplayName()
                : (from != null && from.getUsername() != null ? from.getUsername() : "Mensaje nuevo");

        // Saneo: quita caracteres de control/formato (incl. overrides bidi) que
        // podrían usarse para falsear la notificación, y recorta a 80.
        String rawPreview = req.preview() == null ? "" :
                req.preview().replaceAll("[\\p{Cc}\\p{Cf}]", "").strip();
        String preview = rawPreview.length() > 80 ? rawPreview.substring(0, 80) + "…" : rawPreview;

        Map<String, String> data = new HashMap<>();
        data.put("targetType", "chat");
        data.put("targetId", sender.uid());   // chatId = uid del remitente para que receptor abra ese chat
        data.put("title", fromName);
        data.put("body", preview);

        fcmService.sendToToken(to.getFcmToken(), fromName, preview, data);
        return ResponseEntity.ok().build();
    }
}
