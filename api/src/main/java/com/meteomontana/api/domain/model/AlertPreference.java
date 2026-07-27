package com.meteomontana.api.domain.model;

import java.time.LocalDate;

/**
 * Preferencia de alertas de un usuario (aviso de fin de semana y ventana
 * óptima). Modelo de dominio: los casos de uso de alertas trabajan con esto,
 * no con la entidad JPA.
 *
 * schoolIds y alertDays viajan como CSV igual que en la tabla (formato
 * histórico); quien los necesita los parte.
 */
public record AlertPreference(
        String uid,
        boolean enabled,
        int notifyDay,
        int notifyHour,
        /** CSV de ids de escuelas elegidas a mano (modo SCHOOLS). */
        String schoolIds,
        /** SCHOOLS | NEARBY */
        String mode,
        Integer radiusKm,
        Double userLat,
        Double userLon,
        /** CSV de días ISO a comparar (1=lunes .. 7=domingo). */
        String alertDays,
        boolean optimalEnabled,
        int optimalThreshold,
        /** Último día en que se envió el aviso de ventana óptima (anti-repetición). */
        LocalDate optimalLastSent
) {}
