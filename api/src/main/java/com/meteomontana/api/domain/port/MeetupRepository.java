package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Meetup;

import java.util.List;
import java.util.Optional;

public interface MeetupRepository {

    /** Todas las quedadas activas (no caducadas). El filtro de visibilidad lo aplica el use case. */
    List<Meetup> findActive();

    Optional<Meetup> findById(String id);

    Meetup save(Meetup meetup);

    void delete(String id);

    boolean isMember(String meetupId, String uid);

    void addMember(String meetupId, String uid);

    void removeMember(String meetupId, String uid);

    /** Para el @Scheduled de caducidad. */
    List<Meetup> findExpired();
}
