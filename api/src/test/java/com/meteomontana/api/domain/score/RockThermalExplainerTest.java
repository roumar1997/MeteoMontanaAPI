package com.meteomontana.api.domain.score;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El factor "ROCA" del acordeon: tipo + estado termico + inercia. */
class RockThermalExplainerTest {

    @Test
    void granitoCalienteSuspendeYExplicaLaInercia() {
        var e = RockThermalExplainer.explain("Granito", 28.0, 24.0, 3.0);
        assertEquals("ROCA · GRANITO", e.name());
        assertFalse(e.passes());
        assertTrue(e.display().contains("28°"));
        assertTrue(e.display().contains("~3 h"));
    }

    @Test
    void rocaGuardandoCalorSuspendeAunqueNoQueme() {
        // 24 grados de roca con aire a 18: delta 6 -> sigue soltando calor.
        var e = RockThermalExplainer.explain("Caliza", 24.0, 18.0, 1.8);
        assertFalse(e.passes());
    }

    @Test
    void rocaFriaApruebaConBuenaFriccion() {
        var e = RockThermalExplainer.explain("Granito", 14.0, 13.0, 3.0);
        assertTrue(e.passes());
        assertTrue(e.display().contains("14°"));
    }

    @Test
    void sinTipoDeRocaUsaNombreGenerico() {
        var e = RockThermalExplainer.explain(null, 20.0, 19.0, 1.8);
        assertEquals("ROCA", e.name());
        assertTrue(e.passes());
    }
}
