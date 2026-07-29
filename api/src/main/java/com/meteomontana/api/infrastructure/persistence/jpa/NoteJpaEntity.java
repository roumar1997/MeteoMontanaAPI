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
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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
    //GETTERSSSS

}
