package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.MeetupAlertRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetMeetupAlertUseCase {

    private final MeetupAlertRepository repo;

    public GetMeetupAlertUseCase(MeetupAlertRepository repo) {
        this.repo = repo;
    }

    /** Devuelve la alerta global (schoolId=null) del usuario, o null si no existe. */
    public MeetupAlertDto execute(String uid) {
        Optional<com.meteomontana.api.domain.model.MeetupAlert> alert =
                repo.findByUidAndSchoolId(uid, null);
        return alert.map(a -> new MeetupAlertDto(a.getSchoolId(), a.getDaysCsv())).orElse(null);
    }
}
