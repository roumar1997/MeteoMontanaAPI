package com.meteomontana.api.application.contribution;

/** Body del POST /api/schools/{id}/contributions */
public record ContributionRequest(
        String type,            // PARKING | BOULDER | SECTOR | POSITION_CORRECTION | ASSIGN_SECTOR
        String name,            // opcional
        double lat,             // coordenada propuesta (o posición actual del elemento a corregir)
        double lon,
        String notes,           // opcional
        String description,     // opcional
        Double proposedLat,     // POSITION_CORRECTION: nueva lat propuesta
        Double proposedLon,     // POSITION_CORRECTION: nueva lon propuesta
        String correctionReason,
        String targetBlockId,   // POSITION_CORRECTION: id del school_block a mover (null = la escuela)
                                // BOULDER: id del bloque al que añadir vías (null = piedra nueva)
                                // ASSIGN_SECTOR: id de la piedra a la que asignar sector
        String targetLineId,    // BOULDER: id de la línea existente a corregir (null = añadir vías)
        String sectorBlockId,   // BOULDER: sector al que pertenece la nueva piedra (opcional)
                                // ASSIGN_SECTOR: sector a asignar a la piedra
        String photoUrl,        // BOULDER: URL de Firebase Storage (null si sin foto)
        String bloquesJson,     // BOULDER: JSON array [{name,grade,startType,linePath}]
        String topoLinesJson,   // BOULDER: líneas normalizadas
        String discipline,      // BOULDER (piedra nueva): BOULDER (bloque) / ROUTE (vía)
        String geometry,        // BOULDER: POINT / LINE (muro)
        String path,            // BOULDER+LINE: polilínea JSON [[lat,lon],...]
        String direction        // BOULDER+LINE: "LTR"/"RTL"
) {}
