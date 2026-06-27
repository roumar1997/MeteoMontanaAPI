package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.MeetupAlert;

import java.util.List;
import java.util.Optional;

public interface MeetupAlertRepository {

    Optional<MeetupAlert> findByUidAndSchoolId(String uid, String schoolId);

    /** Todas las alertas que coinciden con la escuela dada (incluye alertas sin escuela). */
    List<MeetupAlert> findBySchoolId(String schoolId);

    MeetupAlert save(MeetupAlert alert);

    void deleteByUidAndSchoolId(String uid, String schoolId);
}
