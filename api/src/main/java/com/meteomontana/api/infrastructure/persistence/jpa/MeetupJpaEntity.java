package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetups")
public class MeetupJpaEntity {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private String id;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "discipline", length = 16)
    private String discipline;

    @Column(nullable = false, length = 16)
    private String privacy = "OPEN";

    @Column(name = "member_limit")
    private Integer memberLimit;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "creator_uid", nullable = false)
    private String creatorUid;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(name = "last_day", nullable = false)
    private LocalDate lastDay;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "meetup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MeetupDayJpaEntity> days = new ArrayList<>();

    @OneToMany(mappedBy = "meetup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MeetupMemberJpaEntity> members = new ArrayList<>();

    protected MeetupJpaEntity() {}

    public MeetupJpaEntity(String id, String schoolId, String name, String discipline,
                           String privacy, Integer memberLimit, String photoUrl,
                           String creatorUid, String conversationId,
                           LocalDate lastDay, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.name = name;
        this.discipline = discipline;
        this.privacy = privacy;
        this.memberLimit = memberLimit;
        this.photoUrl = photoUrl;
        this.creatorUid = creatorUid;
        this.conversationId = conversationId;
        this.lastDay = lastDay;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getId()             { return id; }
    public String getSchoolId()       { return schoolId; }
    public String getName()           { return name; }
    public String getDiscipline()     { return discipline; }
    public String getPrivacy()        { return privacy; }
    public Integer getMemberLimit()   { return memberLimit; }
    public String getPhotoUrl()       { return photoUrl; }
    public String getCreatorUid()     { return creatorUid; }
    public String getConversationId() { return conversationId; }
    public LocalDate getLastDay()     { return lastDay; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MeetupDayJpaEntity> getDays()       { return days; }
    public List<MeetupMemberJpaEntity> getMembers() { return members; }
}
