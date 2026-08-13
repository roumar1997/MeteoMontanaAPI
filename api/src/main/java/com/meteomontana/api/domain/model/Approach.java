package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

/**
 * Camino grabado (o dibujado) del parking a un sector/piedra, con sus
 * chinchetas. Ver APPROACH_DESIGN.md (repo Android) — Fase 1: solo lectura.
 */
@Getter
public class Approach {

    public enum Source { RECORDED, DRAWN, GPX }
    public enum Status { UNVERIFIED, VERIFIED }

    private final String id;
    private final String schoolId;
    private final String fromBlockId;
    private final String toBlockId;
    private final String name;
    private final String pathJson;
    private final Integer distanceM;
    private final Integer ascentM;
    private final Integer durationMin;
    private final Source source;
    private final Status status;
    private final String authorUid;
    private final LocalDateTime createdAt;
    private final List<ApproachPin> pins;

    public Approach(String id, String schoolId, String fromBlockId, String toBlockId,
                     String name, String pathJson, Integer distanceM, Integer ascentM,
                     Integer durationMin, Source source, Status status, String authorUid,
                     LocalDateTime createdAt, List<ApproachPin> pins) {
        this.id = id; this.schoolId = schoolId; this.fromBlockId = fromBlockId;
        this.toBlockId = toBlockId; this.name = name; this.pathJson = pathJson;
        this.distanceM = distanceM; this.ascentM = ascentM; this.durationMin = durationMin;
        this.source = source; this.status = status; this.authorUid = authorUid;
        this.createdAt = createdAt; this.pins = pins;
    }
}
