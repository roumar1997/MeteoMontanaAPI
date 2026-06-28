package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.MeetupReport;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetup_reports")
public class MeetupReportJpaEntity {

    @Id
    @Column
    private String id;

    @Column(name = "meetup_id", nullable = false)
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
    private MeetupReport.Status status;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MeetupReportJpaEntity() {}

    public MeetupReportJpaEntity(String id, String meetupId, String reporterUid, String reportedUid,
                                  MeetupReport.Reason reason, String context,
                                  MeetupReport.Status status, String resolvedBy,
                                  LocalDateTime resolvedAt, LocalDateTime createdAt) {
        this.id = id;
        this.meetupId = meetupId;
        this.reporterUid = reporterUid;
        this.reportedUid = reportedUid;
        this.reason = reason;
        this.context = context;
        this.status = status;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
    }

    public MeetupReport toDomain() {
        return new MeetupReport(id, meetupId, reporterUid, reportedUid, reason,
                                context, status, resolvedBy, resolvedAt, createdAt);
    }

    public String getId()          { return id; }
    public MeetupReport.Status getStatus()   { return status; }
    public String getResolvedBy()  { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void setStatus(MeetupReport.Status status)   { this.status = status; }
    public void setResolvedBy(String resolvedBy)         { this.resolvedBy = resolvedBy; }
    public void setResolvedAt(LocalDateTime resolvedAt)  { this.resolvedAt = resolvedAt; }
}
