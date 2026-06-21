package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.PendingContribution;
import java.time.LocalDateTime;

/** DTO de salida para propuestas de mejora. */
public record ContributionResponse(
        String id,
        String type,
        String status,
        String schoolId,
        String schoolName,
        String name,
        double lat,
        double lon,
        String notes,
        String description,
        String submittedByName,
        String reviewReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String photoUrl,
        String bloquesJson,
        String topoLinesJson,
        String targetBlockId,
        String targetLineId,
        String sectorBlockId,
        Double proposedLat,
        Double proposedLon,
        String correctionReason,
        String geometry,    // BOULDER: POINT / LINE (muro); null si no aplica
        String path,        // BOULDER+LINE: polilínea JSON "[[lat,lon],...]"
        String direction    // BOULDER+LINE: "LTR"/"RTL"
) {
    public static ContributionResponse from(PendingContribution c) {
        return new ContributionResponse(
                c.getId(), c.getType().name(), c.getStatus().name(),
                c.getSchoolId(), c.getSchoolName(), c.getName(),
                c.getLat(), c.getLon(), c.getNotes(), c.getDescription(),
                c.getSubmittedByName(), c.getReviewReason(),
                c.getCreatedAt(), c.getReviewedAt(),
                c.getPhotoUrl(), c.getBloquesJson(), c.getTopoLinesJson(),
                c.getTargetBlockId(), c.getTargetLineId(), c.getSectorBlockId(),
                c.getProposedLat(), c.getProposedLon(), c.getCorrectionReason(),
                c.getGeometry(), c.getPath(), c.getDirection()
        );
    }
}
