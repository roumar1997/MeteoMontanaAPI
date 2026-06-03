package com.meteomontana.api.application.submissions;

/**
 * Body de POST /api/submissions.
 * Solo `name`, `lat`, `lon` son obligatorios — el resto puede completarlo el admin.
 */
public record SubmitSchoolRequest(
        String name,
        String region,
        String style,
        String rockType,
        Double lat,
        Double lon,
        String location,
        String source,
        String notes
) {}
