package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Propuesta de mejora de una escuela existente (parking, piedra, sector, corrección). */
@Getter
public class PendingContribution {

    public enum Type { PARKING, BOULDER, SECTOR, POSITION_CORRECTION, ASSIGN_SECTOR }

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
                                         // ASSIGN_SECTOR: piedra a la que asignar sector
    private final String targetLineId;   // BOULDER: vía existente a corregir (null = añadir vías nuevas)
    private final String sectorBlockId;  // BOULDER: sector al que pertenece la nueva piedra (opcional)
                                         // ASSIGN_SECTOR: sector que se asigna a la piedra existente
    private final String photoUrl;       // BOULDER: URL de Firebase Storage
    private final String bloquesJson;    // BOULDER: JSON array [{name,grade,startType,linePath}]
    private final String topoLinesJson;  // BOULDER: líneas normalizadas (redundante para admin)
    private final String discipline;     // BOULDER (piedra nueva): BOULDER (bloque) / ROUTE (vía); null si no aplica
    private final String geometry;       // BOULDER: POINT / LINE (muro); null si no aplica
    private final String path;           // BOULDER+LINE: polilínea JSON
    private final String direction;      // BOULDER+LINE: "LTR"/"RTL"
    /** Orientación propuesta por el autor al crear la piedra (opcional):
     *  JSON {"block":"NE","faces":{"0":"N"}}. Mutable para no tocar los
     *  constructores (mismo patrón que description). */
    @Setter
    private String orientationsJson;
    private final String submittedByUid;
    private final String submittedByName;
    private final String reviewedByUid;
    private final String reviewReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime reviewedAt;

    /** Constructor de compatibilidad (sin discipline → null). */
    public PendingContribution(String id, Type type, SubmissionStatus status,
                               String schoolId, String schoolName, String name,
                               double lat, double lon, String notes, String description,
                               Double proposedLat, Double proposedLon, String correctionReason,
                               String targetBlockId, String targetLineId, String sectorBlockId,
                               String photoUrl, String bloquesJson, String topoLinesJson,
                               String submittedByUid, String submittedByName,
                               String reviewedByUid, String reviewReason,
                               LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this(id, type, status, schoolId, schoolName, name, lat, lon, notes, description,
             proposedLat, proposedLon, correctionReason, targetBlockId, targetLineId, sectorBlockId,
             photoUrl, bloquesJson, topoLinesJson, null, null, null, null,
             submittedByUid, submittedByName, reviewedByUid, reviewReason, createdAt, reviewedAt);
    }

    /** Constructor de compatibilidad (sin geometry/path/direction → null). */
    public PendingContribution(String id, Type type, SubmissionStatus status,
                               String schoolId, String schoolName, String name,
                               double lat, double lon, String notes, String description,
                               Double proposedLat, Double proposedLon, String correctionReason,
                               String targetBlockId, String targetLineId, String sectorBlockId,
                               String photoUrl, String bloquesJson, String topoLinesJson, String discipline,
                               String submittedByUid, String submittedByName,
                               String reviewedByUid, String reviewReason,
                               LocalDateTime createdAt, LocalDateTime reviewedAt) {
        this(id, type, status, schoolId, schoolName, name, lat, lon, notes, description,
             proposedLat, proposedLon, correctionReason, targetBlockId, targetLineId, sectorBlockId,
             photoUrl, bloquesJson, topoLinesJson, discipline, null, null, null,
             submittedByUid, submittedByName, reviewedByUid, reviewReason, createdAt, reviewedAt);
    }

    public PendingContribution(String id, Type type, SubmissionStatus status,
                               String schoolId, String schoolName, String name,
                               double lat, double lon, String notes, String description,
                               Double proposedLat, Double proposedLon, String correctionReason,
                               String targetBlockId, String targetLineId, String sectorBlockId,
                               String photoUrl, String bloquesJson, String topoLinesJson, String discipline,
                               String geometry, String path, String direction,
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
        this.sectorBlockId = sectorBlockId;
        this.photoUrl = photoUrl;
        this.bloquesJson = bloquesJson;
        this.topoLinesJson = topoLinesJson;
        this.discipline = discipline;
        this.geometry = geometry;
        this.path = path;
        this.direction = direction;
        this.submittedByUid = submittedByUid;
        this.submittedByName = submittedByName;
        this.reviewedByUid = reviewedByUid;
        this.reviewReason = reviewReason;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

}
