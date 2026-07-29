package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDate;
import lombok.Getter;

@Entity
@Table(name = "meetup_days")
@IdClass(MeetupDayId.class)
public class MeetupDayJpaEntity {

    @Id
    @Column(name = "meetup_id")
    @Getter
    private String meetupId;

    @Id
    @Column(name = "day")
    @Getter
    private LocalDate day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", insertable = false, updatable = false)
    private MeetupJpaEntity meetup;

    protected MeetupDayJpaEntity() {}

    public MeetupDayJpaEntity(String meetupId, LocalDate day) {
        this.meetupId = meetupId;
        this.day = day;
    }

}
