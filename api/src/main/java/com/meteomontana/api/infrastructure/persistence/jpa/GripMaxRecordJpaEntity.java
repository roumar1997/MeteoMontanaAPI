package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "grip_max_records")
public class GripMaxRecordJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "grip_type_id", nullable = false)
    private Integer gripTypeId;

    @Column(nullable = false)
    private String hand;

    @Column(name = "max_kg", nullable = false)
    private double maxKg;

    @Column(name = "edge_mm")
    private String edgeMm;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    protected GripMaxRecordJpaEntity() {}

    public GripMaxRecordJpaEntity(String id, String uid, Integer gripTypeId, String hand,
                                   double maxKg, String edgeMm, LocalDateTime measuredAt) {
        this.id = id;
        this.uid = uid;
        this.gripTypeId = gripTypeId;
        this.hand = hand;
        this.maxKg = maxKg;
        this.edgeMm = edgeMm;
        this.measuredAt = measuredAt;
    }

    public String getId()                  { return id; }
    public String getUid()                 { return uid; }
    public Integer getGripTypeId()         { return gripTypeId; }
    public String getHand()                { return hand; }
    public double getMaxKg()               { return maxKg; }
    public String getEdgeMm()              { return edgeMm; }
    public LocalDateTime getMeasuredAt()   { return measuredAt; }
}
