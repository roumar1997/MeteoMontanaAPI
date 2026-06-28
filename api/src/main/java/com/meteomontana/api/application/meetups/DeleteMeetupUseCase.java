package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMeetupUseCase {

    private final MeetupRepository meetupRepository;

    public DeleteMeetupUseCase(MeetupRepository meetupRepository) {
        this.meetupRepository = meetupRepository;
    }

    @Transactional
    public void execute(String uid, String meetupId) {
        Meetup meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("Quedada no encontrada: " + meetupId));
        if (!meetup.getCreatorUid().equals(uid)) {
            throw new IllegalStateException("Solo el organizador puede eliminar la quedada");
        }
        meetupRepository.delete(meetupId);
    }
}
