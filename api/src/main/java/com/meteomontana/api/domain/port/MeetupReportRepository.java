package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.MeetupReport;

import java.util.List;
import java.util.Optional;

public interface MeetupReportRepository {
    MeetupReport save(MeetupReport report);
    Optional<MeetupReport> findById(String id);
    List<MeetupReport> findPending();
    boolean existsByReporterAndMeetup(String reporterUid, String meetupId);

    /** Marca la denuncia como resuelta/descartada por un admin. */
    MeetupReport resolve(String reportId, MeetupReport.Status status,
                         String adminUid, java.time.LocalDateTime resolvedAt);
}
