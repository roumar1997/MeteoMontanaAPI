package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "weekend_alert_prefs")
@Getter
public class WeekendAlertPrefJpaEntity {

    @Id
    private String uid;

    @Column(nullable = false)
    @Setter
    private boolean enabled;

    /** Día del aviso, ISO-8601: 1=lunes .. 7=domingo. */
    @Column(name = "notify_day", nullable = false)
    @Setter
    private int notifyDay;

    /** Hora local Europe/Madrid del aviso, 0-23. */
    @Column(name = "notify_hour", nullable = false)
    @Setter
    private int notifyHour;

    /** CSV de ids de escuela, máx 3 (modo SCHOOLS). Null/vacío en modo NEARBY. */
    @Column(name = "school_ids", length = 300)
    @Setter
    private String schoolIds;

    /** SCHOOLS = escuelas elegidas a mano; NEARBY = las mejores en un radio. */
    @Column(nullable = false, length = 20)
    @Setter
    private String mode = "SCHOOLS";

    @Column(name = "radius_km")
    @Setter
    private Integer radiusKm;

    @Column(name = "user_lat")
    @Setter
    private Double userLat;

    @Column(name = "user_lon")
    @Setter
    private Double userLon;

    /** CSV de días ISO-8601 a comparar (1=lunes .. 7=domingo). Default vie/sáb/dom. */
    @Column(name = "alert_days", nullable = false, length = 20)
    @Setter
    private String alertDays = "5,6,7";

    /** Alerta "ventana óptima hoy": push si una favorita supera el umbral hoy. */
    @Column(name = "optimal_enabled", nullable = false)
    @Setter
    private boolean optimalEnabled = false;

    /** Umbral de score (0-100) que tiene que superar la ventana óptima. */
    @Column(name = "optimal_threshold", nullable = false)
    @Setter
    private int optimalThreshold = 70;

    /** Último día (Europe/Madrid) en que se envió: máximo un push al día. */
    @Column(name = "optimal_last_sent")
    @Setter
    private LocalDate optimalLastSent;

    @Column(name = "updated_at", nullable = false)
    @Setter
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

}
