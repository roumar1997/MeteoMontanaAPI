package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grip_workout_sets")
public class GripWorkoutSetJpaEntity {

    @Id
    private String id;

    @Column(name = "workout_id", nullable = false)
    private String workoutId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Integer reps;

    @Column(name = "work_s", nullable = false)
    private Integer workS;

    @Column(name = "rest_s", nullable = false)
    private Integer restS;

    @Column(name = "grip_type_id", nullable = false)
    private Integer gripTypeId;

    @Column(name = "target_min_pct", nullable = false)
    private Double targetMinPct;

    @Column(name = "target_max_pct", nullable = false)
    private Double targetMaxPct;

    protected GripWorkoutSetJpaEntity() {}

    public GripWorkoutSetJpaEntity(String id, String workoutId, Integer sortOrder, Integer reps,
                                    Integer workS, Integer restS, Integer gripTypeId,
                                    Double targetMinPct, Double targetMaxPct) {
        this.id = id;
        this.workoutId = workoutId;
        this.sortOrder = sortOrder;
        this.reps = reps;
        this.workS = workS;
        this.restS = restS;
        this.gripTypeId = gripTypeId;
        this.targetMinPct = targetMinPct;
        this.targetMaxPct = targetMaxPct;
    }

    public String getId()              { return id; }
    public String getWorkoutId()       { return workoutId; }
    public Integer getSortOrder()      { return sortOrder; }
    public Integer getReps()           { return reps; }
    public Integer getWorkS()          { return workS; }
    public Integer getRestS()          { return restS; }
    public Integer getGripTypeId()     { return gripTypeId; }
    public Double getTargetMinPct()    { return targetMinPct; }
    public Double getTargetMaxPct()    { return targetMaxPct; }
}
