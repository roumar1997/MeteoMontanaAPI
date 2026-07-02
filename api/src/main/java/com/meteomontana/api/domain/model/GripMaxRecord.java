package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/** Tu máximo vigente para un agarre + mano (1 fila = el récord actual). */
public record GripMaxRecord(
        String id, String uid, int gripTypeId, String hand,   // LEFT | RIGHT
        double maxKg, String edgeMm, LocalDateTime measuredAt
) {}
