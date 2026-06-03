package com.meteomontana.api.application.events;

import com.meteomontana.api.domain.model.SubmissionStatus;

/**
 * Evento publicado cuando un admin aprueba/rechaza una submission.
 * Listeners async se encargan de notificar al autor (push, email, etc.).
 */
public record SubmissionReviewedEvent(
        String submissionId,
        String submitterUid,
        String submissionName,
        SubmissionStatus newStatus,
        String reason
) {}
