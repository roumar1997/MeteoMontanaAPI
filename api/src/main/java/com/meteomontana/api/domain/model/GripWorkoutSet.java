package com.meteomontana.api.domain.model;

/** Un set dentro de una plantilla de entreno de agarres. */
public record GripWorkoutSet(
        String id, int sortOrder, int reps, int workS, int restS,
        int gripTypeId, double targetMinPct, double targetMaxPct
) {}
