package com.meteomontana.api.application.contribution;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.meteomontana.api.application.contribution.WallDiffCalculator.*;
import static org.junit.jupiter.api.Assertions.*;

class WallDiffCalculatorTest {

    private ExistingRoute ex(String id, String name, String grade) { return new ExistingRoute(id, name, grade); }
    private ProposedRoute pr(String id, String name, String grade) { return new ProposedRoute(id, name, grade); }

    @Test
    void detectaNuevaMovidaModificadaQuitada() {
        // Actual: [A(6a), B(6b), C(6c)]
        var existing = List.of(ex("A", "Diedro", "6a"), ex("B", "Fisura", "6b"), ex("C", "Placa", "6c"));
        // Propuesto: [B sin cambios, A subida a 6a+, NUEVA, (C omitida = quitada)]
        var proposed = List.of(
                pr("B", "Fisura", "6b"),      // estaba en pos 1, ahora pos 0 → MOVED
                pr("A", "Diedro", "6a+"),     // pos 0 → 1 (MOVED) + grado 6a→6a+ (MODIFIED) → MOVED_MODIFIED
                pr(null, "Travesía", "7a")    // sin id → NEW
        );
        var diff = compute(existing, proposed, true, "LTR", "RTL");

        assertEquals(Status.MOVED, diff.proposed().get(0).status());
        assertEquals(Integer.valueOf(1), diff.proposed().get(0).oldPos());
        assertEquals(Integer.valueOf(0), diff.proposed().get(0).newPos());

        assertEquals(Status.MOVED_MODIFIED, diff.proposed().get(1).status());
        assertEquals("6a", diff.proposed().get(1).oldGrade());
        assertEquals("6a+", diff.proposed().get(1).newGrade());

        assertEquals(Status.NEW, diff.proposed().get(2).status());
        assertNull(diff.proposed().get(2).lineId());

        // C no está en la propuesta → QUITADA.
        assertEquals(1, diff.removed().size());
        assertEquals("C", diff.removed().get(0).lineId());
        assertEquals(Status.REMOVED, diff.removed().get(0).status());

        assertTrue(diff.pathChanged());
        assertEquals("LTR", diff.oldDirection());
        assertEquals("RTL", diff.newDirection());
    }

    @Test
    void sinCambiosTodoSame() {
        var existing = List.of(ex("A", "Uno", "5"), ex("B", "Dos", "6a"));
        var proposed = List.of(pr("A", "Uno", "5"), pr("B", "Dos", "6a"));
        var diff = compute(existing, proposed, false, "LTR", "LTR");
        assertTrue(diff.proposed().stream().allMatch(r -> r.status() == Status.SAME));
        assertTrue(diff.removed().isEmpty());
        assertFalse(diff.pathChanged());
    }

    @Test
    void referenciaAVíaInexistenteEsConflicto() {
        var existing = List.of(ex("A", "Uno", "5"));
        var proposed = List.of(pr("Z", "Fantasma", "6a")); // Z no existe (cambió mientras tanto)
        var diff = compute(existing, proposed, false, "LTR", "LTR");
        assertEquals(Status.CONFLICT, diff.proposed().get(0).status());
        // A no referenciada → quitada.
        assertEquals(1, diff.removed().size());
        assertEquals("A", diff.removed().get(0).lineId());
    }
}
