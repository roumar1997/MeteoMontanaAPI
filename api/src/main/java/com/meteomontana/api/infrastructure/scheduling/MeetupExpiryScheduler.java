package com.meteomontana.api.infrastructure.scheduling;

import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/**
 * Borra las quedadas caducadas cada día a las 02:00 Madrid.
 * El CASCADE de la BD limpia meetup_days y meetup_members.
 * La conversación de Firestore se borra antes de borrar el meetup.
 */
@Component
@RequiredArgsConstructor
public class MeetupExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MeetupExpiryScheduler.class);

    private final MeetupRepository meetupRepository;
    private final ChatRepository chatRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Europe/Madrid")
    public void expireMeetups() {
        var expired = meetupRepository.findExpired();
        if (expired.isEmpty()) return;
        log.info("Borrando {} quedadas caducadas", expired.size());
        for (var meetup : expired) {
            try {
                chatRepository.deleteConversation(meetup.getConversationId());
                meetupRepository.delete(meetup.getId());
                log.info("Quedada borrada: {} ({})", meetup.getName(), meetup.getId());
            } catch (Exception e) {
                log.warn("Error borrando quedada {}: {}", meetup.getId(), e.getMessage());
            }
        }
    }
}
