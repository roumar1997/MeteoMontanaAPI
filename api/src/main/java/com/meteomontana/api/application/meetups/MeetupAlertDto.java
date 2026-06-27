package com.meteomontana.api.application.meetups;

public record MeetupAlertDto(
        String schoolId,   // null = cualquier escuela
        String daysCsv     // null = cualquier día
) {}
