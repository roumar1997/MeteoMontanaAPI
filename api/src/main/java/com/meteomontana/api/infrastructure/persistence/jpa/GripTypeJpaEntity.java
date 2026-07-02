package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grip_types")
public class GripTypeJpaEntity {

    @Id
    private Integer id;

    @Column(name = "finger_group", nullable = false)
    private String fingerGroup;

    @Column(nullable = false)
    private String style;

    protected GripTypeJpaEntity() {}

    public Integer getId()          { return id; }
    public String getFingerGroup()  { return fingerGroup; }
    public String getStyle()        { return style; }
}
