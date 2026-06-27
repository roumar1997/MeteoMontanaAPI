package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.MeetupReport;
import java.time.LocalDateTime;

public record ReportDto(
        String id,
        String meetupId,
        String reporterUid,
        String reportedUid,
        String reason,
        String context,
        String status,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static ReportDto from(MeetupReport r) {
        return new ReportDto(
                r.getId(), r.getMeetupId(), r.getReporterUid(), r.getReportedUid(),
                r.getReason().name(), r.getContext(), r.getStatus().name(),
                r.getResolvedBy(), r.getResolvedAt(), r.getCreatedAt());
    }
}
