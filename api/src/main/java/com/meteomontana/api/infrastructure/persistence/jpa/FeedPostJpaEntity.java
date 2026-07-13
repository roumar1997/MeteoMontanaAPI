package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Post del feed social (pestaña Comunidad): un ascenso publicado (TICK /
 * PROJECT_DONE) o, en el futuro, una piedra/vía nueva aprobada (NEW_BLOCK /
 * NEW_LINE). Guarda snapshot de nombres/grado para pintar la tarjeta sin
 * joins; la foto y el trazo se leen en vivo de block_lines. Ver V53.
 */
@Entity
@Table(name = "feed_posts")
public class FeedPostJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uid", nullable = false)
    private String userUid;

    @Column(name = "school_id", length = 80)
    private String schoolId;

    @Column(name = "school_name", length = 120)
    private String schoolName;

    @Column(name = "block_id", nullable = false, length = 80)
    private String blockId;

    @Column(name = "block_name", length = 160)
    private String blockName;

    @Column(name = "line_id", length = 80)
    private String lineId;

    @Column(name = "line_name", length = 160)
    private String lineName;

    @Column(length = 8)
    private String grade;

    @Column(nullable = false, length = 16)
    private String kind;

    /** Modalidad snapshoteada al publicar: BOULDER | ROUTE (null en posts viejos). */
    @Column(length = 20)
    private String discipline;

    /** Tipo de roca de la escuela snapshoteado al publicar (null en posts viejos). */
    @Column(name = "rock_type", length = 40)
    private String rockType;

    /** Descripción opcional escrita por el autor al publicar (null en posts automáticos). Ver V55. */
    @Column(length = 500)
    private String caption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected FeedPostJpaEntity() {}

    public FeedPostJpaEntity(String userUid, String schoolId, String schoolName,
                             String blockId, String blockName,
                             String lineId, String lineName,
                             String grade, String kind) {
        this.userUid = userUid;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.blockId = blockId;
        this.blockName = blockName;
        this.lineId = lineId;
        this.lineName = lineName;
        this.grade = grade;
        this.kind = kind;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUserUid() { return userUid; }
    public String getSchoolId() { return schoolId; }
    public String getSchoolName() { return schoolName; }
    public String getBlockId() { return blockId; }
    public String getBlockName() { return blockName; }
    public String getLineId() { return lineId; }
    public String getLineName() { return lineName; }
    public String getGrade() { return grade; }
    public String getKind() { return kind; }
    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getRockType() { return rockType; }
    public void setRockType(String rockType) { this.rockType = rockType; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
