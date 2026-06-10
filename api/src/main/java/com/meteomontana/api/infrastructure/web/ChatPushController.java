package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.model.User;
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
    private final FcmService fcmService;

    public ChatPushController(UserRepository userRepository, FcmService fcmService) {
        this.userRepository = userRepository;
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

        User to = userRepository.findByUid(req.toUid()).orElse(null);
        if (to == null || to.getFcmToken() == null || to.getFcmToken().isBlank()) {
            return ResponseEntity.ok().build();   // sin token, simplemente no se manda
        }
        User from = userRepository.findByUid(sender.uid()).orElse(null);
        String fromName = (from != null && from.getDisplayName() != null)
                ? from.getDisplayName()
                : (from != null && from.getUsername() != null ? from.getUsername() : "Mensaje nuevo");

        String preview = req.preview() == null ? "" :
                (req.preview().length() > 80 ? req.preview().substring(0, 80) + "…" : req.preview());

        Map<String, String> data = new HashMap<>();
        data.put("targetType", "chat");
        data.put("targetId", sender.uid());   // chatId = uid del remitente para que receptor abra ese chat
        data.put("title", fromName);
        data.put("body", preview);

        fcmService.sendToToken(to.getFcmToken(), fromName, preview, data);
        return ResponseEntity.ok().build();
    }
}
