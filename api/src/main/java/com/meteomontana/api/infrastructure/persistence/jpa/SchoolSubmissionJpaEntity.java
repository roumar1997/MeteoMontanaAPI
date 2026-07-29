package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SubmissionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "school_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

}
