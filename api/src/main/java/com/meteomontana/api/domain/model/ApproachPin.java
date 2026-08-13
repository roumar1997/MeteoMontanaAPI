package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Chincheta (foto y/o texto) sobre una aproximación — resuelve las
 * bifurcaciones donde todo el mundo se pierde. Ver APPROACH_DESIGN.md §2.3.
 */
@Getter
public class ApproachPin {

    public enum Kind { FORK, LANDMARK, HAZARD, KEY }
    public enum Status { UNVERIFIED, VERIFIED }

    private final String id;
    private final String approachId;
    private final double lat;
    private final double lon;
    private final int positionIdx;
    private final Kind kind;
    private final String message;
    private final String photoPath;
    private final String authorUid;
    private final Status status;
    private final LocalDateTime createdAt;

    public ApproachPin(String id, String approachId, double lat, double lon, int positionIdx,
                        Kind kind, String message, String photoPath, String authorUid,
                        Status status, LocalDateTime createdAt) {
        if (message == null && photoPath == null) {
            throw new IllegalArgumentException("Una chincheta necesita foto o mensaje");
        }
        this.id = id; this.approachId = approachId; this.lat = lat; this.lon = lon;
        this.positionIdx = positionIdx; this.kind = kind; this.message = message;
        this.photoPath = photoPath; this.authorUid = authorUid; this.status = status;
        this.createdAt = createdAt;
    }
}
