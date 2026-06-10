package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/** Propuesta de mejora de una escuela existente (parking, piedra, sector, corrección). */
public class PendingContribution {

    public enum Type { PARKING, BOULDER, SECTOR, POSITION_CORRECTION }

    private final String id;
    private final Type type;
    private final SubmissionStatus status;
    private final String schoolId;
    private final String schoolName;
    private final String name;
    private final double lat;
    private final double lon;
    private final String notes;
    private final String description;
    private final Double proposedLat;
    private final Double proposedLon;
    private final String correctionReason;
    private final String targetBlockId;  // POSITION_CORRECTION: bloque a mover (null = la escuela)
                                         // BOULDER: bloque al que añadir vías (null = nueva piedra)
    private final String targetLineId;   // BOULDER: vía existente a corregir (null = añadir vías nuevas)
    private final String photoUrl;       // BOULDER: URL de Firebase Storage
    private final String bloquesJson;    // BOULDER: JSON array [{name,grade,startType,linePath}]
    private final String topoLinesJson;  // BOULDER: líneas normalizadas (redundante para admin)
    private final String submittedByUid;
    private final String submittedByName;
    private final String reviewedByUid;
    private final String reviewReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;

    public PendingContribution(String id, Type type, SubmissionStatus status,
                               String schoolId, String schoolName, String name,
                               double lat, double lon, String notes, String description,
                               Double proposedLat, Double proposedLon, String correctionReason,
                               String targetBlockId, String targetLineId,
                               String photoUrl, String bloquesJson, String topoLinesJson,
                               String submittedByUid, String submittedByName,
                               String reviewedByUid, String reviewReason,
                               LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.notes = notes;
        this.description = description;
        this.proposedLat = proposedLat;
        this.proposedLon = proposedLon;
        this.correctionReason = correctionReason;
        this.targetBlockId = targetBlockId;
        this.targetLineId = targetLineId;
        this.photoUrl = photoUrl;
        this.bloquesJson = bloquesJson;
        this.topoLinesJson = topoLinesJson;
        this.submittedByUid = submittedByUid;
        this.submittedByName = submittedByName;
        this.reviewedByUid = reviewedByUid;
        this.reviewReason = reviewReason;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public String getId()                { return id; }
    public Type getType()                { return type; }
    public SubmissionStatus getStatus()  { return status; }
    public String getSchoolId()          { return schoolId; }
    public String getSchoolName()        { return schoolName; }
    public String getName()              { return name; }
    public double getLat()               { return lat; }
    public double getLon()               { return lon; }
    public String getNotes()             { return notes; }
    public String getDescription()       { return description; }
    public Double getProposedLat()       { return proposedLat; }
    public Double getProposedLon()       { return proposedLon; }
    public String getCorrectionReason()  { return correctionReason; }
    public String getTargetBlockId()     { return targetBlockId; }
    public String getTargetLineId()      { return targetLineId; }
    public String getPhotoUrl()          { return photoUrl; }
    public String getBloquesJson()       { return bloquesJson; }
    public String getTopoLinesJson()     { return topoLinesJson; }
    public String getSubmittedByUid()    { return submittedByUid; }
    public String getSubmittedByName()   { return submittedByName; }
    public String getReviewedByUid()     { return reviewedByUid; }
    public String getReviewReason()      { return reviewReason; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
