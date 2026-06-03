package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_sessions")
public class JournalSessionJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "school_name")
    private String schoolName;

    private String sector;

    @Column(name = "block_name", nullable = false)
    private String blockName;

    private String grade;
    private String notes;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected JournalSessionJpaEntity() {}

    public JournalSessionJpaEntity(String id, String uid, String schoolId, String schoolName,
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
