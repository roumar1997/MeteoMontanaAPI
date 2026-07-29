package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "meetup_members")
@IdClass(MeetupMemberId.class)
public class MeetupMemberJpaEntity {

    @Id
    @Column(name = "meetup_id")
    @Getter
    private String meetupId;

    @Id
    @Column(name = "uid")
    @Getter
    private String uid;

    @Column(name = "joined_at", nullable = false)
    @Getter
    private LocalDateTime joinedAt;

    @Column(name = "gear_json")
    @Getter
    @Setter
    private String gearJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", insertable = false, updatable = false)
    private MeetupJpaEntity meetup;

    protected MeetupMemberJpaEntity() {}

    public MeetupMemberJpaEntity(String meetupId, String uid, LocalDateTime joinedAt) {
        this.meetupId = meetupId;
        this.uid = uid;
        this.joinedAt = joinedAt;
    }

}
