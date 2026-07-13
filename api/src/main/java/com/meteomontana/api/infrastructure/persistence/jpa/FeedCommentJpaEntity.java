package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Comentario en un post del feed. Separado de line_comments a propósito:
 * "¡qué máquina!" es sobre el ascenso de una persona, no información de la
 * vía (no debe aparecer en la ficha de la piedra). Ver V53.
 */
@Entity
@Table(name = "feed_comments")
public class FeedCommentJpaEntity {

    @Id
    private String id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Comentario al que responde (V57), null = comentario raíz. */
    @Column(name = "parent_id", length = 80)
    private String parentId;

    protected FeedCommentJpaEntity() {}

    public FeedCommentJpaEntity(String id, Long postId, String uid, String author, String text) {
        this(id, postId, uid, author, text, null);
    }

    public FeedCommentJpaEntity(String id, Long postId, String uid, String author,
                                String text, String parentId) {
        this.id = id;
        this.postId = postId;
        this.uid = uid;
        this.author = author;
        this.text = text;
        this.parentId = parentId;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public Long getPostId() { return postId; }
    public String getUid() { return uid; }
    public String getAuthor() { return author; }
    public String getText() { return text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getParentId() { return parentId; }
}
