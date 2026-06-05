package com.meteomontana.api.application.contribution;

/** Body del POST /api/schools/{id}/contributions */
public record ContributionRequest(
        String type,            // PARKING | BOULDER | SECTOR | POSITION_CORRECTION
        String name,            // opcional
        double lat,             // coordenada propuesta (o posición actual del elemento a corregir)
        double lon,
        String notes,           // opcional
        String description,     // opcional
        Double proposedLat,     // POSITION_CORRECTION: nueva lat propuesta
        Double proposedLon,     // POSITION_CORRECTION: nueva lon propuesta
        String correctionReason,
        String targetBlockId    // POSITION_CORRECTION: id del school_block a mover (null = la escuela)
) {}
