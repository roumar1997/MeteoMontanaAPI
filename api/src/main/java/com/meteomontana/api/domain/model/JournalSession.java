package com.meteomontana.api.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
             discipline, lineId, null, sessionDate, createdAt);
    }

    public JournalSession(String id, String uid, String schoolId, String schoolName,
                          String sector, String blockName, String grade, String notes,
                          String discipline, String lineId, String status,
                          LocalDate sessionDate, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.sector = sector;
        this.blockName = blockName;
        this.grade = grade;
        this.notes = notes;
        this.discipline = discipline;
        this.lineId = lineId;
        this.status = status;
        this.sessionDate = sessionDate;
        this.createdAt = createdAt;
    }

    public String getId()              { return id; }
    public String getUid()             { return uid; }
    public String getSchoolId()        { return schoolId; }
    public String getSchoolName()      { return schoolName; }
    public String getSector()          { return sector; }
    public String getBlockName()       { return blockName; }
    public String getGrade()           { return grade; }
    public String getNotes()           { return notes; }
    public String getDiscipline()      { return discipline; }
    public String getLineId()          { return lineId; }
    public String getStatus()          { return status; }
    public LocalDate getSessionDate()  { return sessionDate; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
