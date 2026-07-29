package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KickMemberUseCase {

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public void execute(String requesterUid, String meetupId, String targetUid) {
        var meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("Quedada no encontrada: " + meetupId));

        if (!meetup.getCreatorUid().equals(requesterUid)) {
            throw new IllegalStateException("Solo el creador puede expulsar miembros");
        }
        if (targetUid.equals(requesterUid)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo");
        }

        meetupRepository.removeMember(meetupId, targetUid);

        List<String> participants = new ArrayList<>(chatRepository.participantsOf(meetup.getConversationId()));
        participants.remove(targetUid);
        chatRepository.updateParticipants(meetup.getConversationId(), participants);
    }
}
