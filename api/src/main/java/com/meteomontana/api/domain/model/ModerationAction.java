package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/** Acción de moderación ya aplicada (auditoría con motivo). */
public record ModerationAction(
        String id,
        String adminUid,
        String targetUid,
        /** WARN | SUSPEND | BAN | UNBAN | DELETE_NOTE | DELETE_COMMENT */
        String action,
        String reason,
        String snapshot,
        LocalDateTime createdAt
) {}
