package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.MeetupAlert;
import com.meteomontana.api.domain.port.MeetupAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SetMeetupAlertUseCase {

    private final MeetupAlertRepository repo;

    public SetMeetupAlertUseCase(MeetupAlertRepository repo) {
        this.repo = repo;
    }

    /**
     * Activa (enabled=true) o desactiva (enabled=false) la alerta global del usuario.
     * daysCsv puede ser null (= cualquier día) o CSV de números ISO 1-7.
     */
    @Transactional
    public MeetupAlertDto execute(String uid, boolean enabled, String daysCsv) {
        if (!enabled) {
            repo.deleteByUidAndSchoolId(uid, null);
            return new MeetupAlertDto(null, null);
        }
        // Upsert: eliminar la vieja y guardar la nueva
        repo.deleteByUidAndSchoolId(uid, null);
        MeetupAlert saved = repo.save(new MeetupAlert(
                UUID.randomUUID().toString(), uid, null, daysCsv, LocalDateTime.now()
        ));
        return new MeetupAlertDto(saved.getSchoolId(), saved.getDaysCsv());
    }
}
