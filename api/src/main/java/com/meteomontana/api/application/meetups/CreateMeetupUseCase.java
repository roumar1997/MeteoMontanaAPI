package com.meteomontana.api.application.meetups;

import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.domain.model.MeetupAlert;
import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupAlertRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CreateMeetupUseCase {

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final MeetupAlertRepository alertRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final PushSender pushSender;
    private final MeetupDtoMapper mapper;

    public CreateMeetupUseCase(MeetupRepository meetupRepository,
                               ChatRepository chatRepository,
                               SchoolRepository schoolRepository,
                               UserRepository userRepository,
                               MeetupAlertRepository alertRepository,
                               FollowRepository followRepository,
                               NotificationService notificationService,
                               PushSender pushSender,
                               MeetupDtoMapper mapper) {
        this.meetupRepository = meetupRepository;
        this.chatRepository = chatRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
        this.followRepository = followRepository;
        this.notificationService = notificationService;
        this.pushSender = pushSender;
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

        // Gate "no mixto": el creador debe tener género WOMAN
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
        LocalDateTime expiresAt = lastDay.plusDays(1).atStartOfDay();

        User creator = userRepository.findByUid(creatorUid)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String groupName = req.name() + " · " + (creator.getDisplayName() != null ? creator.getDisplayName() : creatorUid);

        String conversationId = chatRepository.createGroup(creatorUid, groupName, List.of());

        LocalDateTime now = LocalDateTime.now();
        Meetup meetup = new Meetup(
                null, req.schoolId(), req.name(), req.description(),
                req.discipline() != null ? req.discipline().toUpperCase() : null,
                privacy, req.memberLimit(), req.photoUrl(),
                creatorUid, conversationId, sortedDays, lastDay, expiresAt, now,
                List.of(new Meetup.MeetupMember(creatorUid,
                        creator.getUsername(), creator.getDisplayName(),
                        creator.getPhotoPath(), now, null))
        );

        Meetup saved = meetupRepository.save(meetup);

        // Notificar a usuarios con alertas activas (best-effort)
        notifyAlerts(saved, creator, privacy);

        return mapper.toDto(saved, creatorUid);
    }

    private void notifyAlerts(Meetup meetup, User creator, String privacy) {
        try {
            List<MeetupAlert> alerts = alertRepository.findBySchoolId(meetup.getSchoolId());
            School school = schoolRepository.findById(meetup.getSchoolId()).orElse(null);
            String title = "Nueva quedada: " + meetup.getName();
            String creatorName = creator.getDisplayName() != null ? creator.getDisplayName() : creator.getUsername();
            String body = "Organiza " + creatorName;

            for (MeetupAlert alert : alerts) {
                String alertUid = alert.getUid();
                if (alertUid.equals(meetup.getCreatorUid())) continue;
                if (!alert.matchesDays(meetup.getDays())) continue;
                if (!alert.matchesDiscipline(meetup.getDiscipline())) continue;
                if (!alert.matchesPrivacyPreference(privacy)) continue;
                if (school != null && !alert.matchesDistance(school.getLat(), school.getLon())) continue;

                // El usuario debe poder VER la quedada según su privacidad real,
                // no solo según su preferencia de filtro.
                if ("FOLLOWERS".equals(privacy)) {
                    boolean related = followRepository.isFollowing(alertUid, meetup.getCreatorUid())
                            || followRepository.isFollowing(meetup.getCreatorUid(), alertUid);
                    if (!related) continue;
                }
                if ("WOMEN".equals(privacy)) {
                    boolean isWoman = userRepository.findByUid(alertUid)
                            .map(u -> "WOMAN".equals(u.getGender())).orElse(false);
                    if (!isWoman) continue;
                }

                notificationService.create(alertUid, "MEETUP_NEW", title, body, "meetup", meetup.getId());

                userRepository.findByUid(alertUid).ifPresent(u -> {
                    if (u.getFcmToken() != null) {
                        pushSender.sendToToken(u.getFcmToken(), title, body,
                                Map.of("targetType", "meetup", "targetId", meetup.getId()));
                    }
                });
            }
        } catch (Exception ignored) {
            // Las alertas no deben bloquear la creación
        }
    }
}
