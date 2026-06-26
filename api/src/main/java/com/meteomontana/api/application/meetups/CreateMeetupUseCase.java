package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreateMeetupUseCase {

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final MeetupDtoMapper mapper;

    public CreateMeetupUseCase(MeetupRepository meetupRepository,
                               ChatRepository chatRepository,
                               SchoolRepository schoolRepository,
                               UserRepository userRepository,
                               MeetupDtoMapper mapper) {
        this.meetupRepository = meetupRepository;
        this.chatRepository = chatRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MeetupDto execute(String creatorUid, CreateMeetupRequest req) {
        // Validaciones
        if (req.days() == null || req.days().isEmpty())
            throw new IllegalArgumentException("Se necesita al menos un día");
        if (req.days().size() > 7)
            throw new IllegalArgumentException("Máximo 7 días por quedada");
        if (req.name() == null || req.name().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (req.name().length() > 80)
            throw new IllegalArgumentException("Nombre demasiado largo (máx 80)");

        schoolRepository.findById(req.schoolId())
                .orElseThrow(() -> new IllegalArgumentException("Escuela no encontrada: " + req.schoolId()));

        String privacy = req.privacy() != null ? req.privacy().toUpperCase() : "OPEN";
        if (!List.of("OPEN", "FOLLOWERS", "WOMEN").contains(privacy))
            throw new IllegalArgumentException("Privacidad inválida: " + privacy);

        // Gate "solo mujeres": el creador debe tener género WOMAN
        if ("WOMEN".equals(privacy)) {
            User creator = userRepository.findByUid(creatorUid)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            if (!"WOMAN".equals(creator.getGender())) {
                throw new IllegalStateException("GENDER_REQUIRED");
            }
        }

        if (req.discipline() != null) {
            if (!List.of("BOULDER", "ROUTE", "BOTH").contains(req.discipline().toUpperCase()))
                throw new IllegalArgumentException("Disciplina inválida: " + req.discipline());
        }

        List<LocalDate> sortedDays = req.days().stream().sorted().toList();
        LocalDate lastDay = sortedDays.get(sortedDays.size() - 1);
        // Caduca al día siguiente del último día a medianoche Madrid
        LocalDateTime expiresAt = lastDay.plusDays(1).atStartOfDay();

        User creator = userRepository.findByUid(creatorUid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String groupName = req.name() + " · " + (creator.getDisplayName() != null ? creator.getDisplayName() : creatorUid);

        // Crear el grupo de chat en Firestore con el creador como único participante inicial
        String conversationId = chatRepository.createGroup(creatorUid, groupName, List.of());

        LocalDateTime now = LocalDateTime.now();
        Meetup meetup = new Meetup(
                null, req.schoolId(), req.name(),
                req.discipline() != null ? req.discipline().toUpperCase() : null,
                privacy, req.memberLimit(), req.photoUrl(),
                creatorUid, conversationId, sortedDays, lastDay, expiresAt, now,
                List.of(new Meetup.MeetupMember(creatorUid,
                        creator.getUsername(), creator.getDisplayName(),
                        creator.getPhotoPath(), now))
        );

        Meetup saved = meetupRepository.save(meetup);
        return mapper.toDto(saved, creatorUid);
    }
}
