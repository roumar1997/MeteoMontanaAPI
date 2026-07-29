package com.meteomontana.api.application.admin;

import com.meteomontana.api.application.events.SubmissionReviewedEvent;
import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.AdminLogRepository;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RejectSubmissionUseCase {

    private final SchoolSubmissionRepository submissionRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminGuard adminGuard;
    private final ApplicationEventPublisher events;

    @Transactional
    public SubmissionDto execute(String adminUid, String submissionId, String reason) {
        adminGuard.ensureAdmin(adminUid);

        SchoolSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SchoolNotFoundException(submissionId));

        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new IllegalStateException("Submission is not pending: " + submission.getStatus());
        }

        SchoolSubmission updated = new SchoolSubmission(
                submission.getId(),
                submission.getProposedName(), submission.getProposedRegion(),
                submission.getProposedStyle(), submission.getProposedRockType(),
                submission.getProposedLat(), submission.getProposedLon(),
                submission.getProposedLocation(), submission.getProposedSource(),
                submission.getNotes(),
                SubmissionStatus.REJECTED,
                submission.getSubmittedByUid(),
                adminUid, reason, null,
                submission.getCreatedAt(), LocalDateTime.now()
        );
        SchoolSubmission persisted = submissionRepository.save(updated);

        adminLogRepository.save(new AdminLog(
                UUID.randomUUID().toString(),
                adminUid,
                "REJECT_SCHOOL_SUBMISSION",
                "school_submission",
                submission.getId(),
                "Reason: " + (reason != null ? reason : "n/a"),
                LocalDateTime.now()
        ));

        events.publishEvent(new SubmissionReviewedEvent(
                persisted.getId(), persisted.getSubmittedByUid(),
                persisted.getProposedName(), SubmissionStatus.REJECTED, reason
        ));

        return SubmissionDto.from(persisted);
    }
}
