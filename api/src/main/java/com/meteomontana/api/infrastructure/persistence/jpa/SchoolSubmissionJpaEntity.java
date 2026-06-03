package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_submissions")
public class SchoolSubmissionJpaEntity {

    @Id
    private String id;

    @Column(name = "proposed_name", nullable = false)
    private String proposedName;

    @Column(name = "proposed_region")
    private String proposedRegion;

    @Column(name = "proposed_style")
    private String proposedStyle;

    @Column(name = "proposed_rock_type")
    private String proposedRockType;

    @Column(name = "proposed_lat", nullable = false)
    private double proposedLat;

    @Column(name = "proposed_lon", nullable = false)
    private double proposedLon;

    @Column(name = "proposed_location")
    private String proposedLocation;

    @Column(name = "proposed_source")
    private String proposedSource;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "submitted_by_uid", nullable = false)
    private String submittedByUid;

    @Column(name = "reviewed_by_uid")
    private String reviewedByUid;

    @Column(name = "review_reason")
    private String reviewReason;

    @Column(name = "created_school_id")
    private String createdSchoolId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected SchoolSubmissionJpaEntity() {}

    public SchoolSubmissionJpaEntity(String id, String proposedName, String proposedRegion,
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

    public String getId()               { return id; }
    public String getProposedName()     { return proposedName; }
    public String getProposedRegion()   { return proposedRegion; }
    public String getProposedStyle()    { return proposedStyle; }
    public String getProposedRockType() { return proposedRockType; }
    public double getProposedLat()      { return proposedLat; }
    public double getProposedLon()      { return proposedLon; }
    public String getProposedLocation() { return proposedLocation; }
    public String getProposedSource()   { return proposedSource; }
    public String getNotes()            { return notes; }
    public SubmissionStatus getStatus() { return status; }
    public String getSubmittedByUid()   { return submittedByUid; }
    public String getReviewedByUid()    { return reviewedByUid; }
    public String getReviewReason()     { return reviewReason; }
    public String getCreatedSchoolId()  { return createdSchoolId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt(){ return reviewedAt; }
}
