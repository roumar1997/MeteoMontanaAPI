package com.meteomontana.api.infrastructure.persistence.jpa;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
public class NoteJpaEntity {

    @Id
    private String id;
    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notes_school"))
    private SchoolJpaEntity school;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private String author;
    @Column(nullable = false)
    private String uid;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "upvotes_count")
    private int  upvotesCount;
    @Column(name = "downvotes_count")
    private int  downvotesCount;
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    protected NoteJpaEntity(){}

    public NoteJpaEntity(String id, SchoolJpaEntity school, String text, String author, String uid, LocalDateTime createdAt,
                         int upvotesCount, int downvotesCount, String photoUrl){

        this.id = id;
        this.school = school;
        this.text = text;
        this.author = author;
        this.uid = uid;
        this.createdAt = createdAt;
        this.upvotesCount = upvotesCount;
        this.downvotesCount = downvotesCount;
        this.photoUrl = photoUrl;
    }
    //GETTERSSSS

    public String getId() {return id;}
    public SchoolJpaEntity getSchool(){return school;}
    public String getText() {return text;}
    public String getAuthor() {return author;}
    public String getUid() {return uid;}
    public LocalDateTime getCreatedAt() {return createdAt;}
    public int getUpvotesCount() {return upvotesCount;}
    public int getDownvotesCount() {return downvotesCount;}
    public String getPhotoUrl() {return photoUrl;}
}
