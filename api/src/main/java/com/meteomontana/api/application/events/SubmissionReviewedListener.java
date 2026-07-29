package com.meteomontana.api.application.events;

import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.domain.port.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubmissionReviewedListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionReviewedListener.class);

    private final UserRepository userRepository;
    private final PushSender fcmService;

    @Async
    @EventListener
    public void onSubmissionReviewed(SubmissionReviewedEvent event) {
        User submitter = userRepository.findByUid(event.submitterUid()).orElse(null);
        if (submitter == null) {
            log.debug("Submitter {} not found, skipping push", event.submitterUid());
            return;
        }

        String title;
        String body;
        if (event.newStatus() == SubmissionStatus.APPROVED) {
            title = "Propuesta aprobada";
            body  = "Tu propuesta \"" + event.submissionName() + "\" ha sido aprobada.";
        } else {
            title = "Propuesta rechazada";
            String reason = event.reason() != null && !event.reason().isBlank()
                    ? " Motivo: " + event.reason()
                    : "";
            body = "Tu propuesta \"" + event.submissionName() + "\" ha sido rechazada." + reason;
        }

        fcmService.sendToUser(
                submitter.getUid(), title, body,
                Map.of("type", "submission_reviewed", "submissionId", event.submissionId())
        );
    }
}
