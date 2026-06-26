package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaMeetupRepositoryAdapter implements MeetupRepository {

    private final SpringDataMeetupRepository meetupRepo;
    private final UserRepository userRepository;

    public JpaMeetupRepositoryAdapter(SpringDataMeetupRepository meetupRepo,
                                      UserRepository userRepository) {
        this.meetupRepo = meetupRepo;
        this.userRepository = userRepository;
    }

    @Override
    public List<Meetup> findActive() {
        return meetupRepo.findActiveOrderByLastDay(LocalDateTime.now())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Meetup> findById(String id) {
        return meetupRepo.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Meetup save(Meetup meetup) {
        String id = meetup.getId() != null ? meetup.getId() : UUID.randomUUID().toString();
        MeetupJpaEntity entity = new MeetupJpaEntity(
                id, meetup.getSchoolId(), meetup.getName(), meetup.getDiscipline(),
                meetup.getPrivacy(), meetup.getMemberLimit(), meetup.getPhotoUrl(),
                meetup.getCreatorUid(), meetup.getConversationId(),
                meetup.getLastDay(), meetup.getExpiresAt(), meetup.getCreatedAt()
        );
        // Añadir días
        for (LocalDate day : meetup.getDays()) {
            entity.getDays().add(new MeetupDayJpaEntity(id, day));
        }
        // Añadir miembros (el creador ya es miembro)
        if (meetup.getMembers() != null) {
            for (Meetup.MeetupMember m : meetup.getMembers()) {
                entity.getMembers().add(new MeetupMemberJpaEntity(id, m.uid(), LocalDateTime.now()));
            }
        }
        return toDomain(meetupRepo.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        meetupRepo.deleteById(id);
    }

    @Override
    public boolean isMember(String meetupId, String uid) {
        return meetupRepo.findById(meetupId)
                .map(m -> m.getMembers().stream().anyMatch(mm -> mm.getUid().equals(uid)))
                .orElse(false);
    }

    @Override
    @Transactional
    public void addMember(String meetupId, String uid) {
        meetupRepo.findById(meetupId).ifPresent(m -> {
            boolean already = m.getMembers().stream().anyMatch(mm -> mm.getUid().equals(uid));
            if (!already) {
                m.getMembers().add(new MeetupMemberJpaEntity(meetupId, uid, LocalDateTime.now()));
                meetupRepo.save(m);
            }
        });
    }

    @Override
    @Transactional
    public void removeMember(String meetupId, String uid) {
        meetupRepo.findById(meetupId).ifPresent(m -> {
            m.getMembers().removeIf(mm -> mm.getUid().equals(uid));
            meetupRepo.save(m);
        });
    }

    @Override
    public List<Meetup> findExpired() {
        return meetupRepo.findExpired(LocalDateTime.now())
                .stream().map(this::toDomain).toList();
    }

    private Meetup toDomain(MeetupJpaEntity e) {
        List<LocalDate> days = e.getDays().stream().map(MeetupDayJpaEntity::getDay).toList();
        List<Meetup.MeetupMember> members = e.getMembers().stream().map(mm -> {
            var user = userRepository.findByUid(mm.getUid());
            String username = user.map(u -> u.getUsername()).orElse(null);
            String displayName = user.map(u -> u.getDisplayName()).orElse(null);
            String photoUrl = user.map(u -> u.getPhotoPath()).orElse(null);
            return new Meetup.MeetupMember(mm.getUid(), username, displayName, photoUrl, mm.getJoinedAt());
        }).toList();

        return new Meetup(
                e.getId(), e.getSchoolId(), e.getName(), e.getDiscipline(),
                e.getPrivacy(), e.getMemberLimit(), e.getPhotoUrl(),
                e.getCreatorUid(), e.getConversationId(), days,
                e.getLastDay(), e.getExpiresAt(), e.getCreatedAt(), members
        );
    }
}
