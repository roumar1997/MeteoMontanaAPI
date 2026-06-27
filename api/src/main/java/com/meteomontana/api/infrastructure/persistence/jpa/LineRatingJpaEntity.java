package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "line_ratings")
public class LineRatingJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "line_id", nullable = false)
    private String lineId;

    @Column(nullable = false)
    private int stars;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LineRatingJpaEntity() {}

    public LineRatingJpaEntity(String id, String uid, String lineId, int stars, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.lineId = lineId;
        this.stars = stars;
        this.createdAt = createdAt;
    }

    public String getId()                { return id; }
    public String getUid()               { return uid; }
    public String getLineId()            { return lineId; }
    public int getStars()                { return stars; }
    public void setStars(int stars)      { this.stars = stars; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
