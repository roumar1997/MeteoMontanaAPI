package com.meteomontana.api.application.photos;

import java.time.LocalDateTime;

/**
 * DTO de salida: la respuesta al cliente lleva URL firmada en vez de
 * storage path para que el navegador pueda mostrarla directamente.
 */
public record SchoolPhotoDto(
        String id,
        String schoolId,
        String url,                // URL firmada — expira en X minutos
        String uploadedByUid,
        String caption,
        Integer width,
        Integer height,
        Long sizeBytes,
        String contentType,
        LocalDateTime createdAt
) {}
