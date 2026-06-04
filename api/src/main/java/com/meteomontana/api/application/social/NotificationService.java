package com.meteomontana.api.application.social;

import com.meteomontana.api.domain.model.Notification;
import com.meteomontana.api.domain.port.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio que crea entradas en la bandeja de notificaciones.
 * Lo invocan los listeners (follow, submission reviewed, etc.) para registrar
 * la notificación además de mandar el FCM push.
 */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(String uid, String type, String title, String body,
                               String targetType, String targetId) {
        Notification n = new Notification(
                UUID.randomUUID().toString(), uid, type, title, body,
                targetType, targetId, null, LocalDateTime.now()
        );
        return repository.save(n);
    }
}
