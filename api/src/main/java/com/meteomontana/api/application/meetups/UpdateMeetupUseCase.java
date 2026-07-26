package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.exception.ForbiddenException;

import com.meteomontana.api.domain.port.MeetupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Edición de la descripción de una quedada. Solo el organizador puede hacerlo. */
@Service
public class UpdateMeetupUseCase {

    private final MeetupRepository meetupRepository;
    private final MeetupDtoMapper mapper;

    public UpdateMeetupUseCase(MeetupRepository meetupRepository, MeetupDtoMapper mapper) {
        this.meetupRepository = meetupRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MeetupDto updateDescription(String uid, String meetupId, String description) {
        var meetup = meetupRepository.findById(meetupId)
                .orElseThrow(() -> new IllegalArgumentException("Quedada no encontrada: " + meetupId));

        if (!meetup.getCreatorUid().equals(uid)) {
            throw new ForbiddenException("NOT_CREATOR");
        }

        String trimmed = description == null ? null : description.trim();
        if (trimmed != null && trimmed.isEmpty()) trimmed = null;

        meetupRepository.updateDescription(meetupId, trimmed);

        return meetupRepository.findById(meetupId)
                .map(m -> mapper.toDto(m, uid))
                .orElseThrow(() -> new IllegalStateException("Quedada no encontrada tras actualizar"));
    }
}
