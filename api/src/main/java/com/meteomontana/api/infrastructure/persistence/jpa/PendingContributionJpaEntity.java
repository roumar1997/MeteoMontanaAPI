package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SubmissionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_contributions")
public class PendingContributionJpaEntity {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingContribution.Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "school_name", nullable = false)
    private String schoolName;

    private String name;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "proposed_lat")
    private Double proposedLat;

    @Column(name = "proposed_lon")
    private Double proposedLon;

    @Column(name = "correction_reason")
    private String correctionReason;

    @Column(name = "target_block_id")
    private String targetBlockId;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "bloques_json", columnDefinition = "TEXT")
    private String bloquesJson;

    @Column(name = "topo_lines_json", columnDefinition = "TEXT")
    private String topoLinesJson;

    @Column(name = "submitted_by_uid", nullable = false)
    private String submittedByUid;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "reviewed_by_uid")
    private String reviewedByUid;

    @Column(name = "review_reason")
    private String reviewReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    protected PendingContributionJpaEntity() {}

    public static PendingContributionJpaEntity from(PendingContribution c) {
        PendingContributionJpaEntity e = new PendingContributionJpaEntity();
        e.id = c.getId();
        e.type = c.getType();
        e.status = c.getStatus();
        e.schoolId = c.getSchoolId();
        e.schoolName = c.getSchoolName();
        e.name = c.getName();
        e.lat = c.getLat();
        e.lon = c.getLon();
        e.notes = c.getNotes();
        e.description = c.getDescription();
        e.proposedLat = c.getProposedLat();
        e.proposedLon = c.getProposedLon();
        e.correctionReason = c.getCorrectionReason();
        e.targetBlockId = c.getTargetBlockId();
        e.photoUrl = c.getPhotoUrl();
        e.bloquesJson = c.getBloquesJson();
        e.topoLinesJson = c.getTopoLinesJson();
        e.submittedByUid = c.getSubmittedByUid();
        e.submittedByName = c.getSubmittedByName();
        e.reviewedByUid = c.getReviewedByUid();
        e.reviewReason = c.getReviewReason();
        e.createdAt = c.getCreatedAt();
        e.reviewedAt = c.getReviewedAt();
        return e;
    }

    public PendingContribution toDomain() {
        return new PendingContribution(id, type, status, schoolId, schoolName, name,
                lat, lon, notes, description, proposedLat, proposedLon, correctionReason,
                targetBlockId, photoUrl, bloquesJson, topoLinesJson,
                submittedByUid, submittedByName, reviewedByUid, reviewReason,
                createdAt, reviewedAt);
    }

    public String getTargetBlockId() { return targetBlockId; }
    public String getPhotoUrl()      { return photoUrl; }

    // Setters para JPA
    public void setStatus(SubmissionStatus status)       { this.status = status; }
    public void setReviewedByUid(String reviewedByUid)   { this.reviewedByUid = reviewedByUid; }
    public void setReviewReason(String reviewReason)     { this.reviewReason = reviewReason; }
    public void setReviewedAt(LocalDateTime reviewedAt)  { this.reviewedAt = reviewedAt; }

    public String getId()           { return id; }
    public SubmissionStatus getStatus() { return status; }
}
