package com.meteomontana.api.application.contribution;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body del POST /api/schools/{id}/contributions
 *
 * Límites (@Size): todo String que acaba en columna TEXT lleva cap — sin él,
 * un usuario autenticado podía escribir megabytes por fila (seguridad M1).
 * Los JSON de topos llevan margen amplio (una piedra multi-cara con muchas
 * vías ronda decenas de KB; 256 KB es techo de sobra).
 */
public record ContributionRequest(
        @NotBlank @Size(max = 32)
        String type,            // PARKING | BOULDER | SECTOR | POSITION_CORRECTION | ASSIGN_SECTOR | SCHOOL_NAME_CORRECTION
        @Size(max = 120)
        String name,            // opcional; SCHOOL_NAME_CORRECTION: nombre propuesto para la escuela
        @DecimalMin("-90") @DecimalMax("90")
        double lat,             // coordenada propuesta (o posición actual del elemento a corregir)
        @DecimalMin("-180") @DecimalMax("180")
        double lon,
        @Size(max = 1000)
        String notes,           // opcional
        @Size(max = 1000)
        String description,     // opcional
        @DecimalMin("-90") @DecimalMax("90")
        Double proposedLat,     // POSITION_CORRECTION: nueva lat propuesta
        @DecimalMin("-180") @DecimalMax("180")
        Double proposedLon,     // POSITION_CORRECTION: nueva lon propuesta
        @Size(max = 500)
        String correctionReason,
        @Size(max = 64)
        String targetBlockId,   // POSITION_CORRECTION: id del school_block a mover (null = la escuela)
                                // BOULDER: id del bloque al que añadir vías (null = piedra nueva)
                                // ASSIGN_SECTOR: id de la piedra a la que asignar sector
        @Size(max = 64)
        String targetLineId,    // BOULDER: id de la línea existente a corregir (null = añadir vías)
        @Size(max = 64)
        String sectorBlockId,   // BOULDER: sector al que pertenece la nueva piedra (opcional)
                                // ASSIGN_SECTOR: sector a asignar a la piedra
        @Size(max = 500)
        String photoUrl,        // BOULDER: URL de Firebase Storage (null si sin foto)
        @Size(max = 262144)
        String bloquesJson,     // BOULDER: JSON array [{name,grade,startType,linePath}]
        @Size(max = 262144)
        String topoLinesJson,   // BOULDER: líneas normalizadas
        @Size(max = 20)
        String discipline,      // BOULDER (piedra nueva): BOULDER (bloque) / ROUTE (vía)
        @Size(max = 20)
        String geometry,        // BOULDER: POINT / LINE (muro)
        @Size(max = 65536)
        String path,            // BOULDER+LINE: polilínea JSON [[lat,lon],...]
        @Size(max = 8)
        String direction,       // BOULDER+LINE: "LTR"/"RTL"
        @Size(max = 2000)
        String orientationsJson // BOULDER: orientación del autor (opcional):
                                // {"block":"NE","faces":{"0":"N"}} → su primer
                                // voto al aprobarse
) {}
