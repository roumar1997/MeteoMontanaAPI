package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

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

    public Notification(String id, String uid, String type, String title, String body,
                        String targetType, String targetId, LocalDateTime readAt,
                        LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetType = targetType;
        this.targetId = targetId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public String getId()              { return id; }
    public String getUid()             { return uid; }
    public String getType()            { return type; }
    public String getTitle()           { return title; }
    public String getBody()            { return body; }
    public String getTargetType()      { return targetType; }
    public String getTargetId()        { return targetId; }
    public LocalDateTime getReadAt()   { return readAt; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
