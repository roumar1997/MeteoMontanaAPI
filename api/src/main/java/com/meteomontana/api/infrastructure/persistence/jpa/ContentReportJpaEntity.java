package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;

/** Denuncia de contenido (comentario/nota/usuario). Ver V50. */
@Entity
@Table(name = "content_reports")
@Getter
public class ContentReportJpaEntity {

    @Id
    private String id;

    @Column(name = "reporter_uid", nullable = false)
    private String reporterUid;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(nullable = false, length = 30)
    private String reason;

    @Column(length = 1200)
    private String snapshot;

    @Column(name = "author_uid")
    private String authorUid;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 20)
    private String resolution;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected ContentReportJpaEntity() {}

    public ContentReportJpaEntity(String id, String reporterUid, String targetType,
                                  String targetId, String reason, String snapshot,
                                  String authorUid) {
        this.id = id; this.reporterUid = reporterUid; this.targetType = targetType;
        this.targetId = targetId; this.reason = reason; this.snapshot = snapshot;
        this.authorUid = authorUid;
    }

    public void resolve(String resolution) {
        this.status = "RESOLVED";
        this.resolution = resolution;
        this.resolvedAt = LocalDateTime.now();
    }
}
