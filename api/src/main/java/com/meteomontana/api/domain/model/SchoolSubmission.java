package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
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

}
