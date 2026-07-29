package com.meteomontana.api.infrastructure.scheduling;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Equivale a la Cloud Function cleanupOldProposals.
 * Borra submissions APPROVED/REJECTED con más de 5 días de antigüedad,
 * todos los días a las 03:00.
 */
@Component
@RequiredArgsConstructor
public class SubmissionCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubmissionCleanupScheduler.class);
    private static final int RETENTION_DAYS = 5;

    private final SpringDataSchoolSubmissionRepository repository;

    /** Cron Spring: segundo minuto hora dia mes diaSemana. "0 0 3 * * *" = 03:00 todos los días. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Madrid")
    @Transactional
    public void cleanupOldReviewedSubmissions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deleted = 0;

        List<SubmissionStatus> doneStates = List.of(SubmissionStatus.APPROVED, SubmissionStatus.REJECTED);
        for (SubmissionStatus state : doneStates) {
            for (var entity : repository.findByStatusOrderByCreatedAtAsc(state)) {
                LocalDateTime reviewedAt = entity.getReviewedAt();
                if (reviewedAt != null && reviewedAt.isBefore(cutoff)) {
                    repository.deleteById(entity.getId());
                    deleted++;
                }
            }
        }
        log.info("Submission cleanup: deleted {} old reviewed submissions", deleted);
    }
}
