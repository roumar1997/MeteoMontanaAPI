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
    private final LocalDate sessionDate;
    private final LocalDateTime createdAt;

    public JournalSession(String id, String uid, String schoolId, String schoolName,
                          String sector, String blockName, String grade, String notes,
                          LocalDate sessionDate, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.sector = sector;
        this.blockName = blockName;
        this.grade = grade;
        this.notes = notes;
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
    public LocalDate getSessionDate()  { return sessionDate; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
