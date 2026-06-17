package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.model.User;
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
    private final FcmService fcmService;

    public ChatPushController(UserRepository userRepository,
                              FollowRepository followRepository,
                              FcmService fcmService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.fcmService = fcmService;
    }

    public record NotifyRequest(String toUid, String preview) {}

    @PostMapping("/notify")
    public ResponseEntity<Void> notify(
            @AuthenticationPrincipal FirebaseUser sender,
            @RequestBody NotifyRequest req) {
        if (req == null || req.toUid() == null || req.toUid().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // No mandes push a ti mismo
        if (sender.uid().equals(req.toUid())) return ResponseEntity.ok().build();

        // Anti-spam: solo se permite avisar a alguien con quien hay relación de
        // seguimiento (en cualquier sentido). Sin esto, cualquiera podía mandar
        // notificaciones push con texto arbitrario a cualquier UID (que es
        // público). Si no hay relación, se ignora silenciosamente (no se filtra
        // si el otro existe o tiene token).
        boolean related = followRepository.isFollowing(sender.uid(), req.toUid())
                || followRepository.isFollowing(req.toUid(), sender.uid());
        if (!related) return ResponseEntity.ok().build();

        User to = userRepository.findByUid(req.toUid()).orElse(null);
        if (to == null || to.getFcmToken() == null || to.getFcmToken().isBlank()) {
            return ResponseEntity.ok().build();   // sin token, simplemente no se manda
        }
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
