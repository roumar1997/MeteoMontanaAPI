package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Post del feed social (pestaña Comunidad): un ascenso publicado (TICK /
 * PROJECT_DONE) o el post automático al aprobar una contribución (NEW_BLOCK /
 * NEW_LINE). Guarda snapshot de nombres/grado para pintar la tarjeta sin
 * joins; la foto y el trazo se leen en vivo de la piedra al mapear la vista.
 *
 * Modelo de dominio puro (sin JPA): {@code id} y {@code createdAt} son null
 * hasta que la persistencia crea la fila (ver FeedPostRepository.create).
 */
@Getter
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
    @Setter
    private String discipline;
    /** Tipo de roca de la escuela snapshoteado al publicar (null en posts viejos). */
    @Setter
    private String rockType;
    /** Descripción opcional del autor (null en posts automáticos). */
    @Setter
    private String caption;
    /** Ruta en Storage de la foto de celebración opcional (null sin foto). */
    @Setter
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

}
