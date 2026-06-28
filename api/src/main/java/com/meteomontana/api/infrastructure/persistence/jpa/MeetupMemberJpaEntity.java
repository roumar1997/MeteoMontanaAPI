package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetup_members")
@IdClass(MeetupMemberId.class)
public class MeetupMemberJpaEntity {

    @Id
    @Column(name = "meetup_id")
    private String meetupId;

    @Id
    @Column(name = "uid")
    private String uid;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", insertable = false, updatable = false)
    private MeetupJpaEntity meetup;

    protected MeetupMemberJpaEntity() {}

    public MeetupMemberJpaEntity(String meetupId, String uid, LocalDateTime joinedAt) {
        this.meetupId = meetupId;
        this.uid = uid;
        this.joinedAt = joinedAt;
    }

    public String getMeetupId()      { return meetupId; }
    public String getUid()           { return uid; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
