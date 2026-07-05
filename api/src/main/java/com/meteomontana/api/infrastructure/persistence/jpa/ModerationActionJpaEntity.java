package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Una acción de moderación registrada (auditoría con motivo). */
@Entity
@Table(name = "moderation_actions")
public class ModerationActionJpaEntity {

    @Id
    private String id;

    @Column(name = "admin_uid", nullable = false)
    private String adminUid;

    @Column(name = "target_uid")
    private String targetUid;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ModerationActionJpaEntity() {}

    public ModerationActionJpaEntity(String id, String adminUid, String targetUid,
                                     String action, String reason, String snapshot) {
        this.id = id;
        this.adminUid = adminUid;
        this.targetUid = targetUid;
        this.action = action;
        this.reason = reason;
        this.snapshot = snapshot;
        this.createdAt = LocalDateTime.now();
    }

    public String getId()            { return id; }
    public String getAdminUid()      { return adminUid; }
    public String getTargetUid()     { return targetUid; }
    public String getAction()        { return action; }
    public String getReason()        { return reason; }
    public String getSnapshot()      { return snapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
