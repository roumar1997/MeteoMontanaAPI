package com.meteomontana.api.application.meetups;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateMeetupRequest(
        @NotBlank @Size(max = 64)
        String schoolId,
        @NotBlank @Size(max = 120)
        String name,
        @Size(max = 2000)
        String description,    // detalles opcionales del organizador
        @Size(max = 20)
        String discipline,     // BOULDER | ROUTE | BOTH | null
        @Size(max = 20)
        String privacy,        // OPEN | FOLLOWERS | WOMEN
        @Min(2) @Max(500)
        Integer memberLimit,   // null = sin tope
        @Size(max = 500)
        String photoUrl,
        @Size(max = 31)
        List<LocalDate> days
) {}
