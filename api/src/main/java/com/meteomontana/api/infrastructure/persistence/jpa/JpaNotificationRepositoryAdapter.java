package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.Notification;
import com.meteomontana.api.domain.port.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository jpaRepo;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Notification save(Notification n) {
        NotificationJpaEntity e = new NotificationJpaEntity(
                n.getId(), n.getUid(), n.getType(), n.getTitle(), n.getBody(),
                n.getTargetType(), n.getTargetId(), n.getReadAt(), n.getCreatedAt()
        );
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public Optional<Notification> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findByUid(String uid, int limit) {
        return jpaRepo.findByUidOrderByCreatedAtDesc(uid, PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countUnread(String uid) {
        return jpaRepo.countByUidAndReadAtIsNull(uid);
    }

    @Override
    @Transactional
    public void markAsRead(String id) {
        jpaRepo.findById(id).ifPresent(e -> {
            if (e.getReadAt() == null) {
                e.setReadAt(LocalDateTime.now());
                jpaRepo.save(e);
            }
        });
    }

    @Override
    @Transactional
    public void markAllAsRead(String uid) {
        jpaRepo.markAllAsRead(uid, LocalDateTime.now());
    }

    private Notification toDomain(NotificationJpaEntity e) {
        return new Notification(
                e.getId(), e.getUid(), e.getType(), e.getTitle(), e.getBody(),
                e.getTargetType(), e.getTargetId(), e.getReadAt(), e.getCreatedAt()
        );
    }
}
