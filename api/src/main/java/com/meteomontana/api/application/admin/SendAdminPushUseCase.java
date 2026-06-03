package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.AdminLogRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.push.FcmService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SendAdminPushUseCase {

    public record AdminPushRequest(String targetUid, String title, String body) {}
    public record AdminPushResponse(int sent, int recipients) {}

    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final AdminLogRepository adminLogRepository;
    private final AdminGuard adminGuard;

    public SendAdminPushUseCase(UserRepository userRepository, FcmService fcmService,
                                AdminLogRepository adminLogRepository, AdminGuard adminGuard) {
        this.userRepository = userRepository;
        this.fcmService = fcmService;
        this.adminLogRepository = adminLogRepository;
        this.adminGuard = adminGuard;
    }

    public AdminPushResponse execute(String adminUid, AdminPushRequest req) {
        adminGuard.ensureAdmin(adminUid);

        if (req.title() == null || req.body() == null) {
            throw new IllegalArgumentException("title and body are required");
        }

        int sent;
        int recipients;
        String target;

        if (req.targetUid() != null && !req.targetUid().isBlank()) {
            User target_ = userRepository.findByUid(req.targetUid())
                    .orElseThrow(() -> new UserNotFoundException(req.targetUid()));
            boolean ok = fcmService.sendToToken(target_.getFcmToken(), req.title(), req.body(), null);
            sent = ok ? 1 : 0;
            recipients = 1;
            target = "user:" + req.targetUid();
        } else {
            List<User> users = userRepository.findAllWithFcmToken();
            List<String> tokens = users.stream().map(User::getFcmToken).toList();
            sent = fcmService.sendToTokens(tokens, req.title(), req.body(), null);
            recipients = tokens.size();
            target = "broadcast";
        }

        adminLogRepository.save(new AdminLog(
                UUID.randomUUID().toString(), adminUid,
                "SEND_PUSH", "fcm", target,
                "title='" + req.title() + "' sent=" + sent + "/" + recipients,
                LocalDateTime.now()
        ));

        return new AdminPushResponse(sent, recipients);
    }
}
