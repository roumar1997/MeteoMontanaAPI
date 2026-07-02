package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "grip_measure_sessions")
public class GripMeasureSessionJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "grip_type_id", nullable = false)
    private Integer gripTypeId;

    @Column(nullable = false)
    private String hand;

    @Column(name = "peak_kg", nullable = false)
    private double peakKg;

    @Column(name = "avg_kg", nullable = false)
    private double avgKg;

    @Column(name = "duration_s", nullable = false)
    private Integer durationS;

    @Column(name = "edge_mm")
    private String edgeMm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GripMeasureSessionJpaEntity() {}

    public GripMeasureSessionJpaEntity(String id, String uid, Integer gripTypeId, String hand,
                                        double peakKg, double avgKg, Integer durationS,
                                        String edgeMm, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.gripTypeId = gripTypeId;
        this.hand = hand;
        this.peakKg = peakKg;
        this.avgKg = avgKg;
        this.durationS = durationS;
        this.edgeMm = edgeMm;
        this.createdAt = createdAt;
    }

    public String getId()                { return id; }
    public String getUid()               { return uid; }
    public Integer getGripTypeId()       { return gripTypeId; }
    public String getHand()              { return hand; }
    public double getPeakKg()            { return peakKg; }
    public double getAvgKg()             { return avgKg; }
    public Integer getDurationS()        { return durationS; }
    public String getEdgeMm()            { return edgeMm; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
}
