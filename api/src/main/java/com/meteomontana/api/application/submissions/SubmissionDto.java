package com.meteomontana.api.application.submissions;

import com.meteomontana.api.domain.model.SchoolSubmission;
import com.meteomontana.api.domain.model.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionDto(
        String id,
        String proposedName,
        String proposedRegion,
        String proposedStyle,
        String proposedRockType,
        double proposedLat,
        double proposedLon,
        String proposedLocation,
        String proposedSource,
        String notes,
        SubmissionStatus status,
        String submittedByUid,
        String reviewedByUid,
        String reviewReason,
        String createdSchoolId,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String proposedCountry
) {
    public static SubmissionDto from(SchoolSubmission s) {
        return new SubmissionDto(
                s.getId(), s.getProposedName(), s.getProposedRegion(),
                s.getProposedStyle(), s.getProposedRockType(),
                s.getProposedLat(), s.getProposedLon(),
                s.getProposedLocation(), s.getProposedSource(), s.getNotes(),
                s.getStatus(), s.getSubmittedByUid(),
                s.getReviewedByUid(), s.getReviewReason(), s.getCreatedSchoolId(),
                s.getCreatedAt(), s.getReviewedAt(),
                s.getProposedCountry()
        );
    }
}
