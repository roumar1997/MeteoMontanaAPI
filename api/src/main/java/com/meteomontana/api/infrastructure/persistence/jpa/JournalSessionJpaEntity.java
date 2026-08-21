package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "journal_sessions")
@Getter
public class JournalSessionJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String uid;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "school_name")
    private String schoolName;

    private String sector;

    @Column(name = "block_name", nullable = false)
    private String blockName;

    private String grade;
    private String notes;

    // Modalidad de la vía marcada (BOULDER/ROUTE). Snapshot para el conteo del
    // perfil. Se setea aparte (setDiscipline) para no tocar el constructor.
    @Setter
    private String discipline;

    // Id estable de la BlockLine marcada. Enganche para mostrar en vivo y
    // propagar cambios de grado. Se setea aparte (setLineId).
    @Column(name = "line_id")
    @Setter
    private String lineId;

    // DONE (hecho) | PROJECT (proyecto: lo estás probando, aún no te ha salido).
    // Se setea aparte (setStatus) para no tocar el constructor. Default DONE
    // (columna NOT NULL DEFAULT 'DONE' en BD; entradas nuevas sin setStatus
    // explícito también caen aquí).
    @Column(name = "status", nullable = false)
    private String status = "DONE";

    // Estilo de ascensión, independientes entre sí (Rodrigo, 2026-08-21): se
    // puede marcar a vista, al flash, las dos o ninguna.
    @Column(name = "a_vista", nullable = false)
    @Setter
    private boolean aVista = false;

    @Column(name = "al_flash", nullable = false)
    @Setter
    private boolean alFlash = false;

    @Column(name = "session_date", nullable = false)
    @Setter
    private LocalDate sessionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected JournalSessionJpaEntity() {}

    public JournalSessionJpaEntity(String id, String uid, String schoolId, String schoolName,
                                   String sector, String blockName, String grade, String notes,
                                   LocalDate sessionDate, LocalDateTime createdAt) {
        this.id = id;
        this.uid = uid;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.sector = sector;
        this.blockName = blockName;
        this.grade = grade;
        this.notes = notes;
        this.sessionDate = sessionDate;
        this.createdAt = createdAt;
    }

    public void setStatus(String status) { this.status = status != null ? status : "DONE"; }
}
