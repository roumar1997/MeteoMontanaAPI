package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.ContentReport;

import java.util.List;
import java.util.Optional;

/** Denuncias de contenido (puerto de dominio). */
public interface ContentReportRepository {

    List<ContentReport> findByStatus(String status);

    Optional<ContentReport> findById(String id);

    boolean alreadyReported(String reporterUid, String targetType, String targetId);

    /** Nº de denuncias recibidas por un usuario (como autor del contenido). */
    long countByAuthor(String authorUid);

    List<ContentReport> findByAuthor(String authorUid);

    ContentReport create(ContentReport report);

    /** Marca la denuncia como resuelta con esa resolución. */
    void resolve(String id, String status, String resolution, java.time.LocalDateTime resolvedAt);
}
