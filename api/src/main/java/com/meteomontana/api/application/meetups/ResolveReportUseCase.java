package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.MeetupReport;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.MeetupReportJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.MeetupReportJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ResolveReportUseCase {

    private final MeetupReportJpaRepository jpaRepository;
    private final MeetupRepository meetupRepository;

    public ResolveReportUseCase(MeetupReportJpaRepository jpaRepository,
                                MeetupRepository meetupRepository) {
        this.jpaRepository = jpaRepository;
        this.meetupRepository = meetupRepository;
    }

    /** action: "resolve" | "dismiss" | "delete" (borra la quedada denunciada). */
    @Transactional
    public ReportDto execute(String adminUid, String reportId, String action) {
        MeetupReportJpaEntity entity = jpaRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Denuncia no encontrada"));

        // Admin elimina la quedada denunciada (sin exigir ser el creador).
        if ("delete".equalsIgnoreCase(action)) {
            meetupRepository.findById(entity.getMeetupId())
                    .ifPresent(m -> meetupRepository.delete(entity.getMeetupId()));
        }

        MeetupReport.Status newStatus = "dismiss".equalsIgnoreCase(action)
                ? MeetupReport.Status.DISMISSED
                : MeetupReport.Status.RESOLVED;

        entity.setStatus(newStatus);
        entity.setResolvedBy(adminUid);
        entity.setResolvedAt(LocalDateTime.now());

        return ReportDto.from(jpaRepository.save(entity).toDomain());
    }
}
