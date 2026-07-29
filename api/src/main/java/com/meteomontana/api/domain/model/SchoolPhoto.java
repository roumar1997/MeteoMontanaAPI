package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * Foto de una escuela en el dominio. Sabe solo el path en storage,
 * no la URL completa (la genera el StorageService bajo demanda).
 */
@Getter
@AllArgsConstructor
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

}
