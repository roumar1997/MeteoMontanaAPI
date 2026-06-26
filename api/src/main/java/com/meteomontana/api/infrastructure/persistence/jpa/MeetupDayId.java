package com.meteomontana.api.infrastructure.persistence.jpa;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class MeetupDayId implements Serializable {
    private String meetupId;
    private LocalDate day;

    public MeetupDayId() {}
    public MeetupDayId(String meetupId, LocalDate day) { this.meetupId = meetupId; this.day = day; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetupDayId that)) return false;
        return Objects.equals(meetupId, that.meetupId) && Objects.equals(day, that.day);
    }
    @Override public int hashCode() { return Objects.hash(meetupId, day); }
}
