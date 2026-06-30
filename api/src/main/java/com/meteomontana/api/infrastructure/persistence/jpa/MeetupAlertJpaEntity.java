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

    @Column(name = "discipline")
    private String discipline;

    @Column(name = "privacy")
    private String privacy;

    @Column(name = "max_distance_km")
    private Integer maxDistanceKm;

    @Column(name = "user_lat")
    private Double userLat;

    @Column(name = "user_lon")
    private Double userLon;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MeetupAlertJpaEntity() {}

    public MeetupAlertJpaEntity(String id, String uid, String schoolId, String daysCsv,
                                 String discipline, String privacy, Integer maxDistanceKm,
                                 Double userLat, Double userLon, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.schoolId = schoolId;
        this.daysCsv = daysCsv;
        this.discipline = discipline;
        this.privacy = privacy;
        this.maxDistanceKm = maxDistanceKm;
        this.userLat = userLat;
        this.userLon = userLon;
        this.createdAt = createdAt;
    }

    public String getId()                { return id; }
    public String getUid()               { return uid; }
    public String getSchoolId()          { return schoolId; }
    public String getDaysCsv()           { return daysCsv; }
    public void setDaysCsv(String v)     { this.daysCsv = v; }
    public String getDiscipline()        { return discipline; }
    public void setDiscipline(String v)  { this.discipline = v; }
    public String getPrivacy()           { return privacy; }
    public void setPrivacy(String v)     { this.privacy = v; }
    public Integer getMaxDistanceKm()    { return maxDistanceKm; }
    public void setMaxDistanceKm(Integer v) { this.maxDistanceKm = v; }
    public Double getUserLat()           { return userLat; }
    public void setUserLat(Double v)     { this.userLat = v; }
    public Double getUserLon()           { return userLon; }
    public void setUserLon(Double v)     { this.userLon = v; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
