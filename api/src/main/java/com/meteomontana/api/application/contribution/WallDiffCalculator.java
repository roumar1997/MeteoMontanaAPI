package com.meteomontana.api.application.contribution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula el DIFF entre el estado ACTUAL de un muro (sus vías) y el estado
 * COMPLETO propuesto por una contribución, para que el admin vea QUÉ cambia
 * antes de aprobar (no aceptar/rechazar a ciegas).
 *
 * Es lógica PURA (sin JPA ni red): el use case adapta las entidades + el JSON a
 * estas listas y llama a {@link #compute}. Las vías existentes se identifican por
 * `id` estable; las propuestas referencian ese `id` (o `null` si son nuevas). Eso
 * permite distinguir NUEVA vs MOVIDA vs MODIFICADA vs QUITADA.
 */
public final class WallDiffCalculator {

    private WallDiffCalculator() {}

    /** Vía existente en el muro actual (en su orden actual). */
    public record ExistingRoute(String id, String name, String grade) {}

    /** Vía de la propuesta (en el orden propuesto). lineId=null → vía nueva. */
    public record ProposedRoute(String lineId, String name, String grade) {}

    /** Estado de una vía en el diff. */
    public enum Status { NEW, SAME, MOVED, MODIFIED, MOVED_MODIFIED, REMOVED, CONFLICT }

    public record RouteChange(
            Status status,
            String lineId,       // id de la vía (null si NEW)
            String name,
            String oldGrade,
            String newGrade,
            Integer oldPos,      // posición (0-based) en el muro actual; null si NEW
            Integer newPos       // posición propuesta; null si REMOVED
    ) {}

    public record WallDiff(
            boolean pathChanged,
            String oldDirection,
            String newDirection,
            List<RouteChange> proposed,   // en orden propuesto: NEW/SAME/MOVED/MODIFIED/CONFLICT
            List<RouteChange> removed     // existentes que la propuesta omite (QUITADA)
    ) {}

    public static WallDiff compute(List<ExistingRoute> existing, List<ProposedRoute> proposed,
                                   boolean pathChanged, String oldDirection, String newDirection) {
        Map<String, Integer> oldPosById = new LinkedHashMap<>();
        Map<String, ExistingRoute> oldById = new LinkedHashMap<>();
        for (int i = 0; i < existing.size(); i++) {
            ExistingRoute r = existing.get(i);
            oldPosById.put(r.id(), i);
            oldById.put(r.id(), r);
        }

        List<RouteChange> proposedChanges = new ArrayList<>();
        java.util.Set<String> referenced = new java.util.HashSet<>();
        for (int newPos = 0; newPos < proposed.size(); newPos++) {
            ProposedRoute p = proposed.get(newPos);
            String id = p.lineId();
            if (id == null || id.isBlank()) {
                proposedChanges.add(new RouteChange(Status.NEW, null, p.name(),
                        null, p.grade(), null, newPos));
                continue;
            }
            ExistingRoute old = oldById.get(id);
            if (old == null) {
                // Referencia una vía que ya no existe (cambió mientras tanto).
                proposedChanges.add(new RouteChange(Status.CONFLICT, id, p.name(),
                        null, p.grade(), null, newPos));
                continue;
            }
            referenced.add(id);
            int oldPos = oldPosById.get(id);
            boolean moved = oldPos != newPos;
            boolean modified = !eq(old.grade(), p.grade()) || !eq(old.name(), p.name());
            Status st = moved && modified ? Status.MOVED_MODIFIED
                    : moved ? Status.MOVED
                    : modified ? Status.MODIFIED
                    : Status.SAME;
            proposedChanges.add(new RouteChange(st, id, p.name(),
                    old.grade(), p.grade(), oldPos, newPos));
        }

        List<RouteChange> removed = new ArrayList<>();
        for (int i = 0; i < existing.size(); i++) {
            ExistingRoute r = existing.get(i);
            if (!referenced.contains(r.id())) {
                removed.add(new RouteChange(Status.REMOVED, r.id(), r.name(),
                        r.grade(), null, i, null));
            }
        }

        return new WallDiff(pathChanged, oldDirection, newDirection, proposedChanges, removed);
    }

    private static boolean eq(String a, String b) {
        String x = a == null ? "" : a.trim();
        String y = b == null ? "" : b.trim();
        return x.equals(y);
    }
}
