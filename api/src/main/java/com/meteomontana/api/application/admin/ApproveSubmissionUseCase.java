package com.meteomontana.api.application.admin;

import com.meteomontana.api.application.events.SubmissionReviewedEvent;
import com.meteomontana.api.application.submissions.SubmissionDto;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.AdminLog;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.port.AdminLogRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.SchoolSubmissionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aprueba una submission: crea la School en la tabla schools y marca la
 * submission como APPROVED. Todo en una transacción.
 */
@Service
public class ApproveSubmissionUseCase {

    private final SchoolSubmissionRepository submissionRepository;
    private final SchoolRepository schoolRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminGuard adminGuard;
    private final ApplicationEventPublisher events;

    public ApproveSubmissionUseCase(SchoolSubmissionRepository submissionRepository,
                                    SchoolRepository schoolRepository,
                                    AdminLogRepository adminLogRepository,
                                    AdminGuard adminGuard,
                                    ApplicationEventPublisher events) {
        this.submissionRepository = submissionRepository;
        this.schoolRepository = schoolRepository;
        this.adminLogRepository = adminLogRepository;
        this.adminGuard = adminGuard;
        this.events = events;
    }

    @Transactional
    public SubmissionDto execute(String adminUid, String submissionId, String overrideSchoolId) {
        adminGuard.ensureAdmin(adminUid);

        SchoolSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SchoolNotFoundException(submissionId));

        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new IllegalStateException("Submission is not pending: " + submission.getStatus());
        }

        // 1. Crear la escuela
        String schoolId = (overrideSchoolId != null && !overrideSchoolId.isBlank())
                ? overrideSchoolId
                : slug(submission.getProposedName());

        School school = new School(
                schoolId,
                submission.getProposedName(),
                submission.getProposedLocation(),
                submission.getProposedRegion(),
                submission.getProposedStyle(),
                submission.getProposedRockType(),
                submission.getProposedLat(),
                submission.getProposedLon(),
                submission.getProposedSource()
        );
        School saved = schoolRepository.save(school);

        // 2. Actualizar submission
        SchoolSubmission updated = new SchoolSubmission(
                submission.getId(),
                submission.getProposedName(), submission.getProposedRegion(),
                submission.getProposedStyle(), submission.getProposedRockType(),
                submission.getProposedLat(), submission.getProposedLon(),
                submission.getProposedLocation(), submission.getProposedSource(),
                submission.getNotes(),
                SubmissionStatus.APPROVED,
                submission.getSubmittedByUid(),
                adminUid, null, saved.getId(),
                submission.getCreatedAt(), LocalDateTime.now()
        );
        SchoolSubmission persisted = submissionRepository.save(updated);

        // 3. Log de auditoría
        adminLogRepository.save(new AdminLog(
                UUID.randomUUID().toString(),
                adminUid,
                "APPROVE_SCHOOL_SUBMISSION",
                "school_submission",
                submission.getId(),
                "Created school: " + saved.getId(),
                LocalDateTime.now()
        ));

        // 4. Publicar evento (listeners async mandan push)
        events.publishEvent(new SubmissionReviewedEvent(
                persisted.getId(), persisted.getSubmittedByUid(),
                persisted.getProposedName(), SubmissionStatus.APPROVED, null
        ));

        return SubmissionDto.from(persisted);
    }

    private String slug(String name) {
        return name.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("ñ", "n")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
