package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMeetupsUseCase {

    private final MeetupRepository meetupRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MeetupDtoMapper mapper;

    /**
     * @param requesterUid uid del usuario que pide la lista (requerido — el endpoint es auth)
     * @param schoolId     filtro opcional por escuela
     * @param date         filtro opcional: solo quedadas que incluyan este día
     * @param relation     all | following | followers
     */
    public List<MeetupDto> execute(String requesterUid, String schoolId,
                                   LocalDate date, String relation) {
        User requester = userRepository.findByUid(requesterUid).orElse(null);
        String requesterGender = requester != null ? requester.getGender() : null;

        return meetupRepository.findActive().stream()
                .filter(m -> isVisible(m, requesterUid, requesterGender))
                .filter(m -> schoolId == null || m.getSchoolId().equals(schoolId))
                .filter(m -> date == null || m.getDays().contains(date))
                .filter(m -> matchesRelation(m, requesterUid, relation))
                .map(m -> mapper.toDto(m, requesterUid))
                .toList();
    }

    private boolean isVisible(Meetup m, String requesterUid, String requesterGender) {
        // Un MIEMBRO (o el creador) SIEMPRE ve su quedada mientras siga activa,
        // aunque no siga al organizador — p.ej. alguien invitado por enlace a una
        // quedada FOLLOWERS/WOMEN. Sin esto, al refrescar la lista la quedada le
        // desaparecía tras salir del chat, aunque ya se hubiera unido.
        if (isMember(m, requesterUid)) return true;
        return switch (m.getPrivacy()) {
            case "OPEN" -> true;
            case "FOLLOWERS" -> followRepository.isFollowing(requesterUid, m.getCreatorUid()) ||
                    followRepository.isFollowing(m.getCreatorUid(), requesterUid);
            case "WOMEN" -> "WOMAN".equals(requesterGender);
            default -> false;
        };
    }

    private boolean isMember(Meetup m, String requesterUid) {
        if (m.getCreatorUid().equals(requesterUid)) return true;
        return m.getMembers() != null && m.getMembers().stream()
                .anyMatch(mem -> mem.uid().equals(requesterUid));
    }

    private boolean matchesRelation(Meetup m, String requesterUid, String relation) {
        if (relation == null || relation.isBlank() || "all".equalsIgnoreCase(relation)) return true;
        return switch (relation.toLowerCase()) {
            case "following" -> followRepository.isFollowing(requesterUid, m.getCreatorUid())
                    || m.getCreatorUid().equals(requesterUid);
            case "followers" -> followRepository.isFollowing(m.getCreatorUid(), requesterUid)
                    || m.getCreatorUid().equals(requesterUid);
            default -> true;
        };
    }
}
