package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class MeetupReport {

    public enum Status { PENDING, RESOLVED, DISMISSED }
    public enum Reason { SPAM, INAPPROPRIATE, HARASSMENT, OTHER }

    private final String id;
    private final String meetupId;
    private final String reporterUid;
    private final String reportedUid;   // null = denuncia sobre la quedada en sí
    private final Reason reason;
    private final String context;
    private final Status status;
    private final String resolvedBy;
    private final LocalDateTime resolvedAt;
    private final LocalDateTime createdAt;

}
