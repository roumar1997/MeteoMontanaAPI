package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class Note {
    private final String id;
    private final String schoolId;
    private final String text;
    private final String author;
    private final String uid;
    private final LocalDateTime createdAt;
    private final int upvotesCount;
    private final int downvotesCount;
    /** URL pública de la foto adjunta en Firebase Storage. Null si la nota no tiene foto. */
    private final String photoUrl;

}
