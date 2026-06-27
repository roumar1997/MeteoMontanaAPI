package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class MeetupAlert {

    private final String id;
    private final String uid;
    private final String schoolId;   // null = cualquier escuela
    private final String daysCsv;    // "1,2,3" ISO day-of-week, null = cualquier día
    private final LocalDateTime createdAt;

    public MeetupAlert(String id, String uid, String schoolId, String daysCsv, LocalDateTime createdAt) {
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
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** true si esta alerta coincide con alguno de los días de la quedada (ISO 1=L..7=D). */
    public boolean matchesDays(List<java.time.LocalDate> meetupDays) {
        if (daysCsv == null || daysCsv.isBlank()) return true;
        List<String> alertDays = Arrays.asList(daysCsv.split(","));
        return meetupDays.stream().anyMatch(d -> alertDays.contains(String.valueOf(d.getDayOfWeek().getValue())));
    }
}
