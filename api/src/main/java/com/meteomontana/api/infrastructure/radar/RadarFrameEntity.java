package com.meteomontana.api.infrastructure.radar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Un frame archivado del radar de AEMET (ver V45__radar_frames.sql). */
@Entity
@Table(name = "radar_frames")
public class RadarFrameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "radar_code", nullable = false, length = 4)
    private String radarCode;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "image", nullable = false)
    private byte[] image;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    protected RadarFrameEntity() {}

    public RadarFrameEntity(String radarCode, LocalDateTime capturedAt, byte[] image, String sha256) {
        this.radarCode = radarCode;
        this.capturedAt = capturedAt;
        this.image = image;
        this.sha256 = sha256;
    }

    public Long getId() { return id; }
    public String getRadarCode() { return radarCode; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public byte[] getImage() { return image; }
    public String getSha256() { return sha256; }
}
