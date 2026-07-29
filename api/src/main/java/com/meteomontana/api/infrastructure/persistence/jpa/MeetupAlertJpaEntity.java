package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "meetup_alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MeetupAlertJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "days_csv")
    @Setter
    private String daysCsv;

    @Column(name = "discipline")
    @Setter
    private String discipline;

    @Column(name = "privacy")
    @Setter
    private String privacy;

    @Column(name = "max_distance_km")
    @Setter
    private Integer maxDistanceKm;

    @Column(name = "user_lat")
    @Setter
    private Double userLat;

    @Column(name = "user_lon")
    @Setter
    private Double userLon;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
