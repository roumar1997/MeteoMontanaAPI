package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

public class Note {
    private final String id;
    private final String schoolId;
    private final String text;
    private final String author;
    private final String uid;
    private final LocalDateTime createdAt;
    private final int upvotesCount;
    private final int downvotesCount;
    private final String photoUrl; // nullable — foto opcional (Firebase Storage)

    public Note(String id, String schoolId, String text, String author, String uid,
                LocalDateTime createdAt, int upvotesCount, int downvotesCount,
                String photoUrl) {
        this.id = id;
        this.schoolId = schoolId;
        this.text = text;
        this.author = author;
        this.uid = uid;
        this.createdAt = createdAt;
        this.upvotesCount = upvotesCount;
        this.downvotesCount = downvotesCount;
        this.photoUrl = photoUrl;
    }

    public String getId() { return id; }
    public String getSchoolId() { return schoolId; }
    public String getText() { return text; }
    public String getAuthor() { return author; }
    public String getUid() { return uid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getUpvotesCount() { return upvotesCount; }
    public int getDownvotesCount() { return downvotesCount; }
    public String getPhotoUrl() { return photoUrl; }
}