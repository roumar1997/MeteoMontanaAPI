package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SubmissionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pending_contributions")
public class PendingContributionJpaEntity {

    @Id
    @Getter
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PendingContribution.Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    @Setter
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
    @Getter
    private String targetBlockId;

    @Column(name = "target_line_id")
    @Getter
    private String targetLineId;

    @Column(name = "sector_block_id")
    @Getter
    private String sectorBlockId;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    @Getter
    private String photoUrl;

    @Column(name = "bloques_json", columnDefinition = "TEXT")
    @Setter
    private String bloquesJson;

    /** "EDITAR Y APROBAR": el admin puede sustituir el payload por su versión
     *  retocada justo antes de materializar (queda persistido = auditoría). */

    @Column(name = "topo_lines_json", columnDefinition = "TEXT")
    private String topoLinesJson;

    @Column(name = "discipline")
    private String discipline;

    @Column(name = "geometry")
    private String geometry;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Column(name = "wall_direction")
    private String direction;

    @Column(name = "submitted_by_uid", nullable = false)
    private String submittedByUid;

    @Column(name = "submitted_by_name")
    private String submittedByName;

    @Column(name = "reviewed_by_uid")
    @Setter
    private String reviewedByUid;

    @Column(name = "review_reason")
    @Setter
    private String reviewReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    @Setter
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
        e.targetLineId = c.getTargetLineId();
        e.sectorBlockId = c.getSectorBlockId();
        e.photoUrl = c.getPhotoUrl();
        e.bloquesJson = c.getBloquesJson();
        e.topoLinesJson = c.getTopoLinesJson();
        e.discipline = c.getDiscipline();
        e.geometry = c.getGeometry();
        e.path = c.getPath();
        e.direction = c.getDirection();
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
                targetBlockId, targetLineId, sectorBlockId,
                photoUrl, bloquesJson, topoLinesJson, discipline,
                geometry, path, direction,
                submittedByUid, submittedByName, reviewedByUid, reviewReason,
                createdAt, reviewedAt);
    }

    // Setters para JPA

}
