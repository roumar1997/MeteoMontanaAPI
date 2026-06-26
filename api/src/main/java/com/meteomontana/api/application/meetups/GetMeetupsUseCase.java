package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GetMeetupsUseCase {

    private final MeetupRepository meetupRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MeetupDtoMapper mapper;

    public GetMeetupsUseCase(MeetupRepository meetupRepository,
                             FollowRepository followRepository,
                             UserRepository userRepository,
                             MeetupDtoMapper mapper) {
        this.meetupRepository = meetupRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

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
        return switch (m.getPrivacy()) {
            case "OPEN" -> true;
            case "FOLLOWERS" -> m.getCreatorUid().equals(requesterUid) ||
                    followRepository.isFollowing(requesterUid, m.getCreatorUid());
            case "WOMEN" -> "WOMAN".equals(requesterGender);
            default -> false;
        };
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
