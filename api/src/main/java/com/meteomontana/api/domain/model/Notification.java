package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class Notification {
    private final String id;
    private final String uid;
    private final String type;
    private final String title;
    private final String body;
    private final String targetType;
    private final String targetId;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;

}
