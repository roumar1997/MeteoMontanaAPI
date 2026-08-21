package com.meteomontana.api.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class JournalSession {
    private final String id;
    private final String uid;
    private final String schoolId;
    private final String schoolName;
    private final String sector;
    private final String blockName;
    private final String grade;
    private final String notes;
    private final String discipline;   // BOULDER (bloque) / ROUTE (vía); null = BOULDER (entradas antiguas)
    private final String lineId;       // id estable de la BlockLine; null = entrada antigua/offline (match por nombre)
    // DONE (hecho, comportamiento de siempre) | PROJECT (proyecto: lo estás
    // probando pero aún no te ha salido). null = DONE (entradas antiguas).
    private final String status;
    // Estilo de ascensión, independientes entre sí (Rodrigo, 2026-08-21): se
    // puede marcar a vista, al flash, las dos o ninguna. Solo tienen sentido
    // con status=DONE.
    private final boolean aVista;
    private final boolean alFlash;
    private final LocalDate sessionDate;
    private final LocalDateTime createdAt;

    public JournalSession(String id, String uid, String schoolId, String schoolName,
                          String sector, String blockName, String grade, String notes,
                          LocalDate sessionDate, LocalDateTime createdAt) {
        this(id, uid, schoolId, schoolName, sector, blockName, grade, notes,
             null, null, sessionDate, createdAt);
    }

    public JournalSession(String id, String uid, String schoolId, String schoolName,
                          String sector, String blockName, String grade, String notes,
                          String discipline, LocalDate sessionDate, LocalDateTime createdAt) {
        this(id, uid, schoolId, schoolName, sector, blockName, grade, notes,
             discipline, null, sessionDate, createdAt);
    }

    public JournalSession(String id, String uid, String schoolId, String schoolName,
                          String sector, String blockName, String grade, String notes,
                          String discipline, String lineId, LocalDate sessionDate, LocalDateTime createdAt) {
        this(id, uid, schoolId, schoolName, sector, blockName, grade, notes,
             discipline, lineId, null, false, false, sessionDate, createdAt);
    }

}
