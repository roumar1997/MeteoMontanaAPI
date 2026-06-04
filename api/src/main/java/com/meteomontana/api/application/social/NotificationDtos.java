package com.meteomontana.api.application.social;

import com.meteomontana.api.domain.model.Notification;

import java.time.LocalDateTime;

public class NotificationDtos {

    public record NotificationDto(
            String id,
            String type,
            String title,
            String body,
            String targetType,
            String targetId,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        public static NotificationDto from(Notification n) {
            return new NotificationDto(
                    n.getId(), n.getType(), n.getTitle(), n.getBody(),
                    n.getTargetType(), n.getTargetId(),
                    n.getReadAt(), n.getCreatedAt()
            );
        }
    }

    public record InboxDto(long unreadCount, java.util.List<NotificationDto> items) {}

    private NotificationDtos() {}
}
