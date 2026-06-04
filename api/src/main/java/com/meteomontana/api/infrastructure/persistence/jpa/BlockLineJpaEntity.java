package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.BlockLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "block_lines")
public class BlockLineJpaEntity {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "block_id", nullable = false)
    private SchoolBlockJpaEntity block;

    @Column(nullable = false)
    private String name;

    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_type")
    private BlockLine.StartType startType;

    @Column(name = "line_path", columnDefinition = "TEXT")
    private String linePath;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected BlockLineJpaEntity() {}

    public BlockLineJpaEntity(String id, String name, String grade,
                              BlockLine.StartType startType, String linePath, int sortOrder) {
        this.id = id; this.name = name; this.grade = grade;
        this.startType = startType; this.linePath = linePath; this.sortOrder = sortOrder;
    }

    public String getId()             { return id; }
    public SchoolBlockJpaEntity getBlock() { return block; }
    public void setBlock(SchoolBlockJpaEntity b) { this.block = b; }
    public String getName()           { return name; }
    public String getGrade()          { return grade; }
    public BlockLine.StartType getStartType() { return startType; }
    public String getLinePath()       { return linePath; }
    public int getSortOrder()         { return sortOrder; }
}
