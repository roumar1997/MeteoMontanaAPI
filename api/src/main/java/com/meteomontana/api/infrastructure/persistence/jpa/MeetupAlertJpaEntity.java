package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meetup_alerts")
public class MeetupAlertJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "days_csv")
    private String daysCsv;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MeetupAlertJpaEntity() {}

    public MeetupAlertJpaEntity(String id, String uid, String schoolId, String daysCsv, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.schoolId = schoolId;
        this.daysCsv = daysCsv;
        this.createdAt = createdAt;
    }

    public String getId()           { return id; }
    public String getUid()          { return uid; }
    public String getSchoolId()     { return schoolId; }
    public String getDaysCsv()      { return daysCsv; }
    public void setDaysCsv(String v){ this.daysCsv = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
