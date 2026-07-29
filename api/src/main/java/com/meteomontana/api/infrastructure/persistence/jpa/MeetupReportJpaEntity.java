package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.MeetupReport;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "meetup_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetupReportJpaEntity {

    @Id
    @Column
    @Getter
    private String id;

    @Column(name = "meetup_id", nullable = false)
    @Getter
    private String meetupId;

    @Column(name = "reporter_uid", nullable = false)
    private String reporterUid;

    @Column(name = "reported_uid")
    private String reportedUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetupReport.Reason reason;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    @Setter
    private MeetupReport.Status status;

    @Column(name = "resolved_by")
    @Getter
    @Setter
    private String resolvedBy;

    @Column(name = "resolved_at")
    @Getter
    @Setter
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MeetupReport toDomain() {
        return new MeetupReport(id, meetupId, reporterUid, reportedUid, reason,
                                context, status, resolvedBy, resolvedAt, createdAt);
    }

}
