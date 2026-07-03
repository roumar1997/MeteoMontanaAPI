package com.meteomontana.api.application.admin;

import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.AdminLogRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserDeviceRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserDeviceJpaEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SendAdminPushUseCase {

    public record AdminPushRequest(String targetUid, String title, String body) {}
    public record AdminPushResponse(int sent, int recipients) {}

    private final UserRepository userRepository;
    private final PushSender fcmService;
    private final AdminLogRepository adminLogRepository;
    private final AdminGuard adminGuard;
    private final SpringDataUserDeviceRepository deviceRepository;

    public SendAdminPushUseCase(UserRepository userRepository, PushSender fcmService,
                                AdminLogRepository adminLogRepository, AdminGuard adminGuard,
                                SpringDataUserDeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.fcmService = fcmService;
        this.adminLogRepository = adminLogRepository;
        this.adminGuard = adminGuard;
        this.deviceRepository = deviceRepository;
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
            sent = fcmService.sendToUser(target_.getUid(), req.title(), req.body(), null);
            recipients = 1;
            target = "user:" + req.targetUid();
        } else {
            // Broadcast a todos los dispositivos registrados (no un token por usuario).
            List<String> tokens = deviceRepository.findAll().stream()
                    .map(UserDeviceJpaEntity::getToken).toList();
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
