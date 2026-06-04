package com.meteomontana.api.application.social;

import com.meteomontana.api.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationInboxUseCase {

    private final NotificationRepository repository;

    public NotificationInboxUseCase(NotificationRepository repository) {
        this.repository = repository;
    }

    public NotificationDtos.InboxDto inbox(String uid, int limit) {
        var items = repository.findByUid(uid, limit).stream()
                .map(NotificationDtos.NotificationDto::from).toList();
        return new NotificationDtos.InboxDto(repository.countUnread(uid), items);
    }

    public void markRead(String id) {
        repository.markAsRead(id);
    }

    public void markAllRead(String uid) {
        repository.markAllAsRead(uid);
    }
}
