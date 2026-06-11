package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "weekend_alert_prefs")
public class WeekendAlertPrefJpaEntity {

    @Id
    private String uid;

    @Column(nullable = false)
    private boolean enabled;

    /** Día del aviso, ISO-8601: 1=lunes .. 7=domingo. */
    @Column(name = "notify_day", nullable = false)
    private int notifyDay;

    /** Hora local Europe/Madrid del aviso, 0-23. */
    @Column(name = "notify_hour", nullable = false)
    private int notifyHour;

    /** CSV de ids de escuela, máx 3 (modo SCHOOLS). Null/vacío en modo NEARBY. */
    @Column(name = "school_ids", length = 300)
    private String schoolIds;

    /** SCHOOLS = escuelas elegidas a mano; NEARBY = las mejores en un radio. */
    @Column(nullable = false, length = 20)
    private String mode = "SCHOOLS";

    @Column(name = "radius_km")
    private Integer radiusKm;

    @Column(name = "user_lat")
    private Double userLat;

    @Column(name = "user_lon")
    private Double userLon;

    /** CSV de días ISO-8601 a comparar (1=lunes .. 7=domingo). Default vie/sáb/dom. */
    @Column(name = "alert_days", nullable = false, length = 20)
    private String alertDays = "5,6,7";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WeekendAlertPrefJpaEntity() {}

    public WeekendAlertPrefJpaEntity(String uid, boolean enabled, int notifyDay,
                                     int notifyHour, String schoolIds, LocalDateTime updatedAt) {
        this.uid = uid;
        this.enabled = enabled;
        this.notifyDay = notifyDay;
        this.notifyHour = notifyHour;
        this.schoolIds = schoolIds;
        this.updatedAt = updatedAt;
    }

    public String getUid() { return uid; }
    public boolean isEnabled() { return enabled; }
    public int getNotifyDay() { return notifyDay; }
    public int getNotifyHour() { return notifyHour; }
    public String getSchoolIds() { return schoolIds; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getMode() { return mode; }
    public Integer getRadiusKm() { return radiusKm; }
    public Double getUserLat() { return userLat; }
    public Double getUserLon() { return userLon; }

    public void setMode(String mode) { this.mode = mode; }
    public void setRadiusKm(Integer radiusKm) { this.radiusKm = radiusKm; }
    public void setUserLat(Double userLat) { this.userLat = userLat; }
    public void setUserLon(Double userLon) { this.userLon = userLon; }

    public String getAlertDays() { return alertDays; }
    public void setAlertDays(String alertDays) { this.alertDays = alertDays; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setNotifyDay(int notifyDay) { this.notifyDay = notifyDay; }
    public void setNotifyHour(int notifyHour) { this.notifyHour = notifyHour; }
    public void setSchoolIds(String schoolIds) { this.schoolIds = schoolIds; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
