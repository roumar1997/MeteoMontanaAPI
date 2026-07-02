package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;

/** Un test de "Medir" guardado en el historial (para la gráfica de progreso). */
public record GripMeasureSession(
        String id, String uid, int gripTypeId, String hand,
        double peakKg, double avgKg, int durationS, String edgeMm,
        LocalDateTime createdAt
) {}
