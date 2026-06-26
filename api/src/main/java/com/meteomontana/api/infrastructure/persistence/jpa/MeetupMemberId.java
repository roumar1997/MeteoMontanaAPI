package com.meteomontana.api.infrastructure.persistence.jpa;

import java.io.Serializable;
import java.util.Objects;

public class MeetupMemberId implements Serializable {
    private String meetupId;
    private String uid;

    public MeetupMemberId() {}
    public MeetupMemberId(String meetupId, String uid) { this.meetupId = meetupId; this.uid = uid; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeetupMemberId that)) return false;
        return Objects.equals(meetupId, that.meetupId) && Objects.equals(uid, that.uid);
    }
    @Override public int hashCode() { return Objects.hash(meetupId, uid); }
}
