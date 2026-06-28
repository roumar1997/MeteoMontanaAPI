package com.meteomontana.api.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    public Meetup(String id, String schoolId, String name, String description, String discipline,
                  String privacy, Integer memberLimit, String photoUrl,
                  String creatorUid, String conversationId, List<LocalDate> days,
                  LocalDate lastDay, LocalDateTime expiresAt, LocalDateTime createdAt,
                  List<MeetupMember> members) {
        this.id = id;
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        this.discipline = discipline;
        this.privacy = privacy;
        this.memberLimit = memberLimit;
        this.photoUrl = photoUrl;
        this.creatorUid = creatorUid;
        this.conversationId = conversationId;
        this.days = days;
        this.lastDay = lastDay;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.members = members;
    }

    public String getId()             { return id; }
    public String getSchoolId()       { return schoolId; }
    public String getName()           { return name; }
    public String getDescription()    { return description; }
    public String getDiscipline()     { return discipline; }
    public String getPrivacy()        { return privacy; }
    public Integer getMemberLimit()   { return memberLimit; }
    public String getPhotoUrl()       { return photoUrl; }
    public String getCreatorUid()     { return creatorUid; }
    public String getConversationId() { return conversationId; }
    public List<LocalDate> getDays()  { return days; }
    public LocalDate getLastDay()     { return lastDay; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MeetupMember> getMembers() { return members; }

    public boolean isFull() {
        return memberLimit != null && members != null && members.size() >= memberLimit;
    }

    public record MeetupMember(String uid, String username, String displayName,
                                String photoUrl, LocalDateTime joinedAt) {}
}
