package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.exception.ForbiddenException;

import com.meteomontana.api.domain.model.MeetupAlert;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.MeetupAlertRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SetMeetupAlertUseCase {

    private final MeetupAlertRepository repo;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    /** Activa (enabled=true) o desactiva (enabled=false) la alerta global del usuario. */
    @Transactional
    /** Comando de entrada del caso de uso (la web mapea su DTO a esto). */
    public record SetAlertCommand(
            boolean enabled, String daysCsv, String schoolId, String discipline,
            String privacy, Integer maxDistanceKm, Double userLat, Double userLon) {}

    public MeetupAlertDto execute(String uid, SetAlertCommand req) {
        if (!req.enabled()) {
            repo.deleteByUidAndSchoolId(uid, null);
            return new MeetupAlertDto(false, null, null, null, null, null, null, null, null);
        }

        if (req.schoolId() != null) {
            schoolRepository.findById(req.schoolId())
                    .orElseThrow(() -> new IllegalArgumentException("Escuela no encontrada: " + req.schoolId()));
        }

        if (req.discipline() != null && !List.of("BOULDER", "ROUTE", "BOTH").contains(req.discipline().toUpperCase()))
            throw new IllegalArgumentException("Disciplina inválida: " + req.discipline());

        String privacy = req.privacy() != null ? req.privacy().toUpperCase() : null;
        if (privacy != null && !List.of("OPEN", "FOLLOWERS", "WOMEN").contains(privacy))
            throw new IllegalArgumentException("Privacidad inválida: " + privacy);

        // Gate "no mixto": solo usuarias con género Mujer pueden filtrar por WOMEN.
        if ("WOMEN".equals(privacy)) {
            User user = userRepository.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            if (!"WOMAN".equals(user.getGender())) {
                throw new ForbiddenException("GENDER_REQUIRED");
            }
        }

        // Upsert: eliminar la vieja y guardar la nueva
        repo.deleteByUidAndSchoolId(uid, null);
        MeetupAlert saved = repo.save(new MeetupAlert(
                UUID.randomUUID().toString(), uid, req.schoolId(), req.daysCsv(),
                req.discipline() != null ? req.discipline().toUpperCase() : null,
                privacy, req.maxDistanceKm(), req.userLat(), req.userLon(),
                LocalDateTime.now()
        ));

        String schoolName = saved.getSchoolId() != null
                ? schoolRepository.findById(saved.getSchoolId()).map(s -> s.getName()).orElse(null)
                : null;
        return new MeetupAlertDto(true, saved.getSchoolId(), schoolName, saved.getDaysCsv(),
                saved.getDiscipline(), saved.getPrivacy(), saved.getMaxDistanceKm(),
                saved.getUserLat(), saved.getUserLon());
    }
}
