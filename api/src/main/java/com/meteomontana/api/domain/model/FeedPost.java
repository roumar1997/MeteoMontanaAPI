package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/**
 * Post del feed social (pestaña Comunidad): un ascenso publicado (TICK /
 * PROJECT_DONE) o el post automático al aprobar una contribución (NEW_BLOCK /
 * NEW_LINE). Guarda snapshot de nombres/grado para pintar la tarjeta sin
 * joins; la foto y el trazo se leen en vivo de la piedra al mapear la vista.
 *
 * Modelo de dominio puro (sin JPA): {@code id} y {@code createdAt} son null
 * hasta que la persistencia crea la fila (ver FeedPostRepository.create).
 */
public class FeedPost {

    private final Long id;
    private final String userUid;
    private final String schoolId;
    private final String schoolName;
    private final String blockId;
    private final String blockName;
    private final String lineId;
    private final String lineName;
    private final String grade;
    private final String kind;
    private final LocalDateTime createdAt;

    /** Modalidad snapshoteada al publicar: BOULDER | ROUTE (null en posts viejos). */
    private String discipline;
    /** Tipo de roca de la escuela snapshoteado al publicar (null en posts viejos). */
    private String rockType;
    /** Descripción opcional del autor (null en posts automáticos). */
    private String caption;
    /** Ruta en Storage de la foto de celebración opcional (null sin foto). */
    private String photoPath;

    public FeedPost(Long id, String userUid, String schoolId, String schoolName,
                    String blockId, String blockName, String lineId, String lineName,
                    String grade, String kind, LocalDateTime createdAt) {
        this.id = id;
        this.userUid = userUid;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.blockId = blockId;
        this.blockName = blockName;
        this.lineId = lineId;
        this.lineName = lineName;
        this.grade = grade;
        this.kind = kind;
        this.createdAt = createdAt;
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
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getDiscipline() { return discipline; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public String getRockType() { return rockType; }
    public void setRockType(String rockType) { this.rockType = rockType; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
