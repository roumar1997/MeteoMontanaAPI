package com.meteomontana.api.application.meetups;

public record MeetupAlertDto(
        boolean enabled,
        String schoolId,        // null = cualquier escuela
        String schoolName,
        String daysCsv,         // null = cualquier día
        String discipline,      // BOULDER | ROUTE | BOTH | null = cualquiera
        String privacy,         // OPEN | FOLLOWERS | WOMEN | null = cualquiera
        Integer maxDistanceKm,  // null = sin límite
        Double userLat,
        Double userLon
) {}
