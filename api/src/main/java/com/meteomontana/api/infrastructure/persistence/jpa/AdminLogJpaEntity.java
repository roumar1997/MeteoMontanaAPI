package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_logs")
public class AdminLogJpaEntity {

    @Id
    private String id;

    @Column(name = "actor_uid", nullable = false)
    private String actorUid;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminLogJpaEntity() {}

    public AdminLogJpaEntity(String id, String actorUid, String action, String targetType,
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
