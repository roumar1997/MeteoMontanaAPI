package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.exception.ConflictException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.MeetupReport;
import com.meteomontana.api.domain.port.MeetupReportRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubmitReportUseCase {

    private final MeetupReportRepository reportRepository;
    private final MeetupRepository meetupRepository;

    public SubmitReportUseCase(MeetupReportRepository reportRepository,
                                MeetupRepository meetupRepository) {
        this.reportRepository = reportRepository;
        this.meetupRepository = meetupRepository;
    }

    public ReportDto execute(String reporterUid, String meetupId, SubmitReportRequest req) {
        meetupRepository.findById(meetupId)
                .orElseThrow(() -> new NotFoundException("Quedada no encontrada"));

        if (reportRepository.existsByReporterAndMeetup(reporterUid, meetupId)) {
            throw new ConflictException("Ya has denunciado esta quedada");
        }

        MeetupReport.Reason reason;
        try {
            reason = MeetupReport.Reason.valueOf(req.getReason() != null ? req.getReason() : "OTHER");
        } catch (IllegalArgumentException e) {
            reason = MeetupReport.Reason.OTHER;
        }

        MeetupReport report = new MeetupReport(
                null, meetupId, reporterUid, req.getReportedUid(),
                reason, req.getContext(), MeetupReport.Status.PENDING,
                null, null, LocalDateTime.now());

        return ReportDto.from(reportRepository.save(report));
    }
}
