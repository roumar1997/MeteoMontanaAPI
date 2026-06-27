package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

public class MeetupReport {

    public enum Status { PENDING, RESOLVED, DISMISSED }
    public enum Reason { SPAM, INAPPROPRIATE, HARASSMENT, OTHER }

    private final String id;
    private final String meetupId;
    private final String reporterUid;
    private final String reportedUid;   // null = denuncia sobre la quedada en sí
    private final Reason reason;
    private final String context;
    private final Status status;
    private final String resolvedBy;
    private final LocalDateTime resolvedAt;
    private final LocalDateTime createdAt;

    public MeetupReport(String id, String meetupId, String reporterUid, String reportedUid,
                        Reason reason, String context, Status status,
                        String resolvedBy, LocalDateTime resolvedAt, LocalDateTime createdAt) {
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

    public String getId()            { return id; }
    public String getMeetupId()      { return meetupId; }
    public String getReporterUid()   { return reporterUid; }
    public String getReportedUid()   { return reportedUid; }
    public Reason getReason()        { return reason; }
    public String getContext()       { return context; }
    public Status getStatus()        { return status; }
    public String getResolvedBy()    { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
