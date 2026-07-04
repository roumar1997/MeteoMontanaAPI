package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Comentario de la comunidad en una piedra/muro (lineId=null) o en una vía
 * concreta (lineId = block_lines.id). Con contadores de utilidad agregados,
 * como las notas de escuela (ver V49).
 */
@Entity
@Table(name = "line_comments")
public class LineCommentJpaEntity {

    @Id
    private String id;

    @Column(name = "block_id", nullable = false, length = 80)
    private String blockId;

    @Column(name = "line_id", length = 80)
    private String lineId;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "upvotes_count", nullable = false)
    private int upvotesCount;

    @Column(name = "downvotes_count", nullable = false)
    private int downvotesCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LineCommentJpaEntity() {}

    public LineCommentJpaEntity(String id, String blockId, String lineId,
                                String uid, String author, String text) {
        this.id = id;
        this.blockId = blockId;
        this.lineId = lineId;
        this.uid = uid;
        this.author = author;
        this.text = text;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getBlockId() { return blockId; }
    public String getLineId() { return lineId; }
    public String getUid() { return uid; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public int getUpvotesCount() { return upvotesCount; }
    public int getDownvotesCount() { return downvotesCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
