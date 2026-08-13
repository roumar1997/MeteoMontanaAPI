package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.ApproachPin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approach_pins")
@Getter
public class ApproachPinJpaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approach_id", nullable = false)
    @Setter
    private ApproachJpaEntity approach;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "position_idx", nullable = false)
    private int positionIdx;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproachPin.Kind kind;

    private String message;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "author_uid", nullable = false)
    private String authorUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproachPin.Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ApproachPinJpaEntity() {}

    public ApproachPinJpaEntity(String id, double lat, double lon, int positionIdx,
                                 ApproachPin.Kind kind, String message, String photoPath,
                                 String authorUid, ApproachPin.Status status, LocalDateTime createdAt) {
        this.id = id; this.lat = lat; this.lon = lon; this.positionIdx = positionIdx;
        this.kind = kind; this.message = message; this.photoPath = photoPath;
        this.authorUid = authorUid; this.status = status; this.createdAt = createdAt;
    }
}
