package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "school_photos")
public class SchoolPhotoJpaEntity {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_school_photos_school"))
    private SchoolJpaEntity school;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "uploaded_by_uid", nullable = false)
    private String uploadedByUid;

    private String caption;
    private Integer width;
    private Integer height;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected SchoolPhotoJpaEntity() {}

    public SchoolPhotoJpaEntity(String id, SchoolJpaEntity school, String storagePath,
                                String uploadedByUid, String caption,
                                Integer width, Integer height, Long sizeBytes,
                                String contentType, LocalDateTime createdAt) {
        this.id = id;
        this.school = school;
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
    public SchoolJpaEntity getSchool(){ return school; }
    public String getStoragePath()    { return storagePath; }
    public String getUploadedByUid()  { return uploadedByUid; }
    public String getCaption()        { return caption; }
    public Integer getWidth()         { return width; }
    public Integer getHeight()        { return height; }
    public Long getSizeBytes()        { return sizeBytes; }
    public String getContentType()    { return contentType; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
