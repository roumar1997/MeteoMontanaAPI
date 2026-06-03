package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

public class SchoolSubmission {
    private final String id;
    private final String proposedName;
    private final String proposedRegion;
    private final String proposedStyle;
    private final String proposedRockType;
    private final double proposedLat;
    private final double proposedLon;
    private final String proposedLocation;
    private final String proposedSource;
    private final String notes;
    private final SubmissionStatus status;
    private final String submittedByUid;
    private final String reviewedByUid;
    private final String reviewReason;
    private final String createdSchoolId;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;

    public SchoolSubmission(String id, String proposedName, String proposedRegion,
                            String proposedStyle, String proposedRockType,
                            double proposedLat, double proposedLon,
                            String proposedLocation, String proposedSource, String notes,
                            SubmissionStatus status, String submittedByUid,
                            String reviewedByUid, String reviewReason, String createdSchoolId,
                            LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.proposedName = proposedName;
        this.proposedRegion = proposedRegion;
        this.proposedStyle = proposedStyle;
        this.proposedRockType = proposedRockType;
        this.proposedLat = proposedLat;
        this.proposedLon = proposedLon;
        this.proposedLocation = proposedLocation;
        this.proposedSource = proposedSource;
        this.notes = notes;
        this.status = status;
        this.submittedByUid = submittedByUid;
        this.reviewedByUid = reviewedByUid;
        this.reviewReason = reviewReason;
        this.createdSchoolId = createdSchoolId;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public String getId()                  { return id; }
    public String getProposedName()        { return proposedName; }
    public String getProposedRegion()      { return proposedRegion; }
    public String getProposedStyle()       { return proposedStyle; }
    public String getProposedRockType()    { return proposedRockType; }
    public double getProposedLat()         { return proposedLat; }
    public double getProposedLon()         { return proposedLon; }
    public String getProposedLocation()    { return proposedLocation; }
    public String getProposedSource()      { return proposedSource; }
    public String getNotes()               { return notes; }
    public SubmissionStatus getStatus()    { return status; }
    public String getSubmittedByUid()      { return submittedByUid; }
    public String getReviewedByUid()       { return reviewedByUid; }
    public String getReviewReason()        { return reviewReason; }
    public String getCreatedSchoolId()     { return createdSchoolId; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getReviewedAt()   { return reviewedAt; }
}
