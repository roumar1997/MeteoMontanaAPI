package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class AdminLog {
    private final String id;
    private final String actorUid;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String details;
    private final LocalDateTime createdAt;

}
