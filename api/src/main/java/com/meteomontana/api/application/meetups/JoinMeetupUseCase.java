package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class JoinMeetupUseCase {

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MeetupDtoMapper mapper;

    public JoinMeetupUseCase(MeetupRepository meetupRepository,
                             ChatRepository chatRepository,
                             FollowRepository followRepository,
                             UserRepository userRepository,
                             MeetupDtoMapper mapper) {
        this.meetupRepository = meetupRepository;
        this.chatRepository = chatRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MeetupDto execute(String uid, String meetupId) {
        return execute(uid, meetupId, false);
    }

    /** invited=true (enlace de invitación válido): salta la restricción de
     *  seguimiento (FOLLOWERS) pero NUNCA la de género (WOMEN). */
    @Transactional
    public MeetupDto execute(String uid, String meetupId, boolean invited) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("Quedada no encontrada: " + meetupId));

        // Ya es miembro
        if (meetupRepository.isMember(meetupId, uid)) {
            return mapper.toDto(meetup, uid);
        }

        // Comprobar privacidad
        switch (meetup.getPrivacy()) {
            case "FOLLOWERS" -> {
                if (!invited && !meetup.getCreatorUid().equals(uid) &&
                        !followRepository.isFollowing(uid, meetup.getCreatorUid()) &&
                        !followRepository.isFollowing(meetup.getCreatorUid(), uid)) {
                    throw new IllegalStateException("FOLLOW_REQUIRED");
                }
            }
            case "WOMEN" -> {
                User user = userRepository.findByUid(uid)
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                if (!"WOMAN".equals(user.getGender())) {
                    throw new IllegalStateException("GENDER_REQUIRED");
                }
            }
        }

        // Comprobar límite
        if (meetup.isFull()) {
            throw new IllegalStateException("MEETUP_FULL");
        }

        // Añadir a Postgres
        meetupRepository.addMember(meetupId, uid);

        // Añadir a Firestore participants
        List<String> participants = new ArrayList<>(chatRepository.participantsOf(meetup.getConversationId()));
        if (!participants.contains(uid)) {
            participants.add(uid);
            chatRepository.updateParticipants(meetup.getConversationId(), participants);
        }

        Meetup updated = meetupRepository.findById(meetupId).orElse(meetup);
        return mapper.toDto(updated, uid);
    }
}
