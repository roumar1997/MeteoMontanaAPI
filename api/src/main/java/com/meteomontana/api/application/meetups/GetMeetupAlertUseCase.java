package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.MeetupAlertRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMeetupAlertUseCase {

    private final MeetupAlertRepository repo;
    private final SchoolRepository schoolRepository;

    /** Devuelve la alerta global (schoolId=null en la búsqueda de la fila base) del usuario, o vacía si no existe. */
    public MeetupAlertDto execute(String uid) {
        // Buscamos cualquier alerta del usuario (puede estar ligada a una escuela o no).
        Optional<com.meteomontana.api.domain.model.MeetupAlert> alert =
                repo.findByUidAndSchoolId(uid, null);
        if (alert.isEmpty()) {
            return new MeetupAlertDto(false, null, null, null, null, null, null, null, null);
        }
        var a = alert.get();
        String schoolName = a.getSchoolId() != null
                ? schoolRepository.findById(a.getSchoolId()).map(s -> s.getName()).orElse(null)
                : null;
        return new MeetupAlertDto(true, a.getSchoolId(), schoolName, a.getDaysCsv(),
                a.getDiscipline(), a.getPrivacy(), a.getMaxDistanceKm(), a.getUserLat(), a.getUserLon());
    }
}
