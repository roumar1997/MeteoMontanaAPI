package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.MeetupReport;
import com.meteomontana.api.domain.port.MeetupReportRepository;
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

    public ResolveReportUseCase(MeetupReportJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** action: "resolve" | "dismiss" */
    @Transactional
    public ReportDto execute(String adminUid, String reportId, String action) {
        MeetupReportJpaEntity entity = jpaRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Denuncia no encontrada"));

        MeetupReport.Status newStatus = "dismiss".equalsIgnoreCase(action)
                ? MeetupReport.Status.DISMISSED
                : MeetupReport.Status.RESOLVED;

        entity.setStatus(newStatus);
        entity.setResolvedBy(adminUid);
        entity.setResolvedAt(LocalDateTime.now());

        return ReportDto.from(jpaRepository.save(entity).toDomain());
    }
}
