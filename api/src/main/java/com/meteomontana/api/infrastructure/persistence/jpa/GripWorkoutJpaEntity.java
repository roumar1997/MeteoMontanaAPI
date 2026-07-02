package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "grip_workouts")
public class GripWorkoutJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private String name;

    @Column(name = "hand_mode", nullable = false)
    private String handMode;

    @Column(name = "count_mode", nullable = false)
    private String countMode;

    @Column(name = "rest_between_sets_s", nullable = false)
    private Integer restBetweenSetsS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GripWorkoutJpaEntity() {}

    public GripWorkoutJpaEntity(String id, String uid, String name, String handMode, String countMode,
                                 Integer restBetweenSetsS, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.handMode = handMode;
        this.countMode = countMode;
        this.restBetweenSetsS = restBetweenSetsS;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId()                    { return id; }
    public String getUid()                   { return uid; }
    public String getName()                  { return name; }
    public String getHandMode()              { return handMode; }
    public String getCountMode()             { return countMode; }
    public Integer getRestBetweenSetsS()     { return restBetweenSetsS; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public LocalDateTime getUpdatedAt()      { return updatedAt; }
}
