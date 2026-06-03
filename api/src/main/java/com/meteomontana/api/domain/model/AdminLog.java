package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

public class AdminLog {
    private final String id;
    private final String actorUid;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String details;
    private final LocalDateTime createdAt;

    public AdminLog(String id, String actorUid, String action, String targetType,
                    String targetId, String details, LocalDateTime createdAt) {
        this.id = id;
        this.actorUid = actorUid;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public String getId()              { return id; }
    public String getActorUid()        { return actorUid; }
    public String getAction()          { return action; }
    public String getTargetType()      { return targetType; }
    public String getTargetId()        { return targetId; }
    public String getDetails()         { return details; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
