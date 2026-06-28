package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "meetup_days")
@IdClass(MeetupDayId.class)
public class MeetupDayJpaEntity {

    @Id
    @Column(name = "meetup_id")
    private String meetupId;

    @Id
    @Column(name = "day")
    private LocalDate day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetup_id", insertable = false, updatable = false)
    private MeetupJpaEntity meetup;

    protected MeetupDayJpaEntity() {}

    public MeetupDayJpaEntity(String meetupId, LocalDate day) {
        this.meetupId = meetupId;
        this.day = day;
    }

    public String getMeetupId() { return meetupId; }
    public LocalDate getDay()   { return day; }
}
