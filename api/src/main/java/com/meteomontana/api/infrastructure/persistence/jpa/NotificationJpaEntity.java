package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    private String body;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NotificationJpaEntity() {}

    public NotificationJpaEntity(String id, String uid, String type, String title, String body,
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

    public void setReadAt(LocalDateTime v) { this.readAt = v; }
}
