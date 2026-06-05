package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SchoolBlock;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_blocks")
public class SchoolBlockJpaEntity {

    @Id
    private String id;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolBlock.Type type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "photo_path")
    private String photoPath;

    private String description;

    @Column(name = "created_by_uid", nullable = false)
    private String createdByUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<BlockLineJpaEntity> lines = new ArrayList<>();

    protected SchoolBlockJpaEntity() {}

    public SchoolBlockJpaEntity(String id, String schoolId, SchoolBlock.Type type, String name,
                                double lat, double lon, String photoPath, String description,
                                String createdByUid, LocalDateTime createdAt) {
        this.id = id; this.schoolId = schoolId; this.type = type; this.name = name;
        this.lat = lat; this.lon = lon; this.photoPath = photoPath;
        this.description = description; this.createdByUid = createdByUid; this.createdAt = createdAt;
    }

    public String getId()             { return id; }
    public String getSchoolId()       { return schoolId; }
    public SchoolBlock.Type getType() { return type; }
    public String getName()           { return name; }
    public double getLat()            { return lat; }
    public double getLon()            { return lon; }
    public String getPhotoPath()      { return photoPath; }
    public String getDescription()    { return description; }
    public String getCreatedByUid()   { return createdByUid; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public List<BlockLineJpaEntity> getLines() { return lines; }

    public void setLat(double lat) { this.lat = lat; }
    public void setLon(double lon) { this.lon = lon; }

    public void addLine(BlockLineJpaEntity line) {
        line.setBlock(this);
        lines.add(line);
    }
}
