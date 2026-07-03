package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Token FCM de UN dispositivo de un usuario (ver V48). */
@Entity
@Table(name = "user_devices")
public class UserDeviceJpaEntity {

    @Id
    @Column(length = 500)
    private String token;

    @Column(nullable = false)
    private String uid;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected UserDeviceJpaEntity() {}

    public UserDeviceJpaEntity(String token, String uid) {
        this.token = token;
        this.uid = uid;
        this.updatedAt = LocalDateTime.now();
    }

    public String getToken() { return token; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; this.updatedAt = LocalDateTime.now(); }
}
