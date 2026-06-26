package com.meteomontana.api.application.meetups;

import java.time.LocalDate;
import java.util.List;

public record CreateMeetupRequest(
        String schoolId,
        String name,
        String discipline,     // BOULDER | ROUTE | BOTH | null
        String privacy,        // OPEN | FOLLOWERS | WOMEN
        Integer memberLimit,   // null = sin tope
        String photoUrl,
        List<LocalDate> days
) {}
