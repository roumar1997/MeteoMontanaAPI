package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaveMeetupUseCase {

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;

    public LeaveMeetupUseCase(MeetupRepository meetupRepository, ChatRepository chatRepository) {
        this.meetupRepository = meetupRepository;
        this.chatRepository = chatRepository;
    }

    @Transactional
    public void execute(String uid, String meetupId) {
        var meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("Quedada no encontrada: " + meetupId));

        // El creador no puede salir (debería borrar la quedada)
        if (meetup.getCreatorUid().equals(uid)) {
            throw new IllegalStateException("El creador no puede salir. Borra la quedada.");
        }

        meetupRepository.removeMember(meetupId, uid);

        List<String> participants = new ArrayList<>(chatRepository.participantsOf(meetup.getConversationId()));
        participants.remove(uid);
        chatRepository.updateParticipants(meetup.getConversationId(), participants);
    }
}
