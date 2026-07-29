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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "block_lines")
@Getter
public class BlockLineJpaEntity {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "block_id", nullable = false)
    @Setter
    private SchoolBlockJpaEntity block;

    @Column(nullable = false)
    @Setter
    private String name;

    @Setter
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_type")
    @Setter
    private BlockLine.StartType startType;

    @Column(name = "line_path", columnDefinition = "TEXT")
    @Setter
    private String linePath;

    @Column(name = "sort_order", nullable = false)
    @Setter
    private int sortOrder;

    @Column(name = "photo_path", columnDefinition = "TEXT")
    @Setter
    private String photoPath;

    @Column(name = "face_order", nullable = false)
    @Setter
    private int faceOrder;

    @Column(length = 500)
    @Setter
    private String description;

    @Column(length = 60)
    @Setter
    private String variant;

    @Column(name = "setter_grade", length = 8)
    @Setter
    private String setterGrade;

    protected BlockLineJpaEntity() {}

    public BlockLineJpaEntity(String id, String name, String grade,
                              BlockLine.StartType startType, String linePath, int sortOrder) {
        this(id, name, grade, startType, linePath, sortOrder, null, 0);
    }

    public BlockLineJpaEntity(String id, String name, String grade,
                              BlockLine.StartType startType, String linePath, int sortOrder,
                              String photoPath, int faceOrder) {
        this.id = id; this.name = name; this.grade = grade;
        this.startType = startType; this.linePath = linePath; this.sortOrder = sortOrder;
        this.photoPath = photoPath; this.faceOrder = faceOrder;
    }

}
