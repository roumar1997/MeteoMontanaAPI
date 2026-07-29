package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/** Denuncia de contenido (comentario, nota, post…) en la cola del admin. */
public record ContentReport(
        String id,
        String reporterUid,
        String targetType,
        String targetId,
        String reason,
        String snapshot,
        String authorUid,
        String status,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {}
