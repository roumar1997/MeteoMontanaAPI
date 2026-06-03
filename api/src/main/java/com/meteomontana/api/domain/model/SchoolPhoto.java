package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/**
 * Foto de una escuela en el dominio. Sabe solo el path en storage,
 * no la URL completa (la genera el StorageService bajo demanda).
 */
public class SchoolPhoto {
    private final String id;
    private final String schoolId;
    private final String storagePath;
    private final String uploadedByUid;
    private final String caption;
    private final Integer width;
    private final Integer height;
    private final Long sizeBytes;
    private final String contentType;
    private final LocalDateTime createdAt;

    public SchoolPhoto(String id, String schoolId, String storagePath, String uploadedByUid,
                       String caption, Integer width, Integer height, Long sizeBytes,
                       String contentType, LocalDateTime createdAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.storagePath = storagePath;
        this.uploadedByUid = uploadedByUid;
        this.caption = caption;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public String getId()             { return id; }
    public String getSchoolId()       { return schoolId; }
    public String getStoragePath()    { return storagePath; }
    public String getUploadedByUid()  { return uploadedByUid; }
    public String getCaption()        { return caption; }
    public Integer getWidth()         { return width; }
    public Integer getHeight()        { return height; }
    public Long getSizeBytes()        { return sizeBytes; }
    public String getContentType()    { return contentType; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
