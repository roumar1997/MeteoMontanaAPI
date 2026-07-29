package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.MeetupReport;
import com.meteomontana.api.domain.port.MeetupReportRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResolveReportUseCase {

    private final MeetupReportRepository reportRepository;
    private final MeetupRepository meetupRepository;

    /** action: "resolve" | "dismiss" | "delete" (borra la quedada denunciada). */
    @Transactional
    public ReportDto execute(String adminUid, String reportId, String action) {
        MeetupReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Denuncia no encontrada"));

        // Admin elimina la quedada denunciada (sin exigir ser el creador).
        if ("delete".equalsIgnoreCase(action)) {
            meetupRepository.findById(report.getMeetupId())
                    .ifPresent(m -> meetupRepository.delete(report.getMeetupId()));
        }

        MeetupReport.Status newStatus = "dismiss".equalsIgnoreCase(action)
                ? MeetupReport.Status.DISMISSED
                : MeetupReport.Status.RESOLVED;

        return ReportDto.from(
                reportRepository.resolve(reportId, newStatus, adminUid, LocalDateTime.now()));
    }
}
