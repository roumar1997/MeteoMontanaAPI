package com.meteomontana.api.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class MeetupAlert {

    private final String id;
    private final String uid;
    private final String schoolId;       // null = cualquier escuela
    private final String daysCsv;        // CSV de fechas ISO "yyyy-MM-dd", null = cualquier día
    private final String discipline;     // BOULDER | ROUTE | BOTH | null = cualquiera
    private final String privacy;        // OPEN | FOLLOWERS | WOMEN | null = cualquiera
    private final Integer maxDistanceKm; // null = sin límite
    private final Double userLat;
    private final Double userLon;
    private final LocalDateTime createdAt;

    public MeetupAlert(String id, String uid, String schoolId, String daysCsv, LocalDateTime createdAt) {
        this(id, uid, schoolId, daysCsv, null, null, null, null, null, createdAt);
    }

    public MeetupAlert(String id, String uid, String schoolId, String daysCsv,
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

    public String getId()               { return id; }
    public String getUid()              { return uid; }
    public String getSchoolId()         { return schoolId; }
    public String getDaysCsv()          { return daysCsv; }
    public String getDiscipline()       { return discipline; }
    public String getPrivacy()          { return privacy; }
    public Integer getMaxDistanceKm()   { return maxDistanceKm; }
    public Double getUserLat()          { return userLat; }
    public Double getUserLon()          { return userLon; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** true si esta alerta coincide con alguno de los días de la quedada (fechas ISO yyyy-MM-dd). */
    public boolean matchesDays(List<LocalDate> meetupDays) {
        if (daysCsv == null || daysCsv.isBlank()) return true;
        List<String> alertDays = Arrays.asList(daysCsv.split(","));
        return meetupDays.stream().anyMatch(d -> alertDays.contains(d.toString()));
    }

    /** true si la disciplina de la quedada encaja con la preferencia de la alerta. */
    public boolean matchesDiscipline(String meetupDiscipline) {
        if (discipline == null || discipline.isBlank()) return true;
        if (meetupDiscipline == null || "BOTH".equalsIgnoreCase(meetupDiscipline)) return true;
        return discipline.equalsIgnoreCase(meetupDiscipline);
    }

    /** true si el tipo de privacidad de la alerta encaja (null = cualquiera). */
    public boolean matchesPrivacyPreference(String meetupPrivacy) {
        if (privacy == null || privacy.isBlank()) return true;
        return privacy.equalsIgnoreCase(meetupPrivacy);
    }

    /** true si la escuela de la quedada está dentro del radio configurado (o sin límite). */
    public boolean matchesDistance(double schoolLat, double schoolLon) {
        if (maxDistanceKm == null || userLat == null || userLon == null) return true;
        double km = haversineKm(userLat, userLon, schoolLat, schoolLon);
        return km <= maxDistanceKm;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
