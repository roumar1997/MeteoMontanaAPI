package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(String id);
    List<Notification> findByUid(String uid, int limit);
    long countUnread(String uid);
    void markAsRead(String id);
    void markAllAsRead(String uid);
    /** Borra una notificación SOLO si pertenece a ese uid (seguridad). */
    void delete(String id, String uid);
    /** Borra todas las notificaciones del usuario. */
    void deleteAll(String uid);
}
