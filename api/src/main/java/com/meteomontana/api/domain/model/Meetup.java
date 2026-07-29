package com.meteomontana.api.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class Meetup {

    private final String id;
    private final String schoolId;
    private final String name;
    private final String description;      // detalles opcionales del organizador
    private final String discipline;       // BOULDER | ROUTE | BOTH | null
    private final String privacy;          // OPEN | FOLLOWERS | WOMEN
    private final Integer memberLimit;     // null = sin tope
    private final String photoUrl;
    private final String creatorUid;
    private final String conversationId;
    private final List<LocalDate> days;
    private final LocalDate lastDay;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private final List<MeetupMember> members;

    public boolean isFull() {
        return memberLimit != null && members != null && members.size() >= memberLimit;
    }

    public record MeetupMember(String uid, String username, String displayName,
                                String photoUrl, LocalDateTime joinedAt, String gearJson) {}
}
