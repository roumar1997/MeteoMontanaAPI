package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

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

    /** CSV de ids de escuela, máx 3. */
    @Column(name = "school_ids", nullable = false, length = 300)
    private String schoolIds;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WeekendAlertPrefJpaEntity() {}

    public WeekendAlertPrefJpaEntity(String uid, boolean enabled, int notifyDay,
                                     int notifyHour, String schoolIds, Instant updatedAt) {
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
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setNotifyDay(int notifyDay) { this.notifyDay = notifyDay; }
    public void setNotifyHour(int notifyHour) { this.notifyHour = notifyHour; }
    public void setSchoolIds(String schoolIds) { this.schoolIds = schoolIds; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
