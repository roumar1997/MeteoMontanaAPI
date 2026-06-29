package com.meteomontana.api.application.meetups;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MeetupDto(
        String id,
        String schoolId,
        String schoolName,
        Double schoolLat,
        Double schoolLon,
        String name,
        String description,
        String discipline,
        String privacy,
        Integer memberLimit,
        int memberCount,
        String photoUrl,
        String creatorUid,
        String creatorUsername,
        String creatorPhotoUrl,
        String conversationId,
        List<LocalDate> days,
        LocalDate lastDay,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        List<MemberDto> members,
        boolean joined
) {
    public record MemberDto(String uid, String username, String displayName, String photoUrl, String gearJson) {}
}
