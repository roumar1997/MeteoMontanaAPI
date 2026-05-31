package com.meteomontana.api.domain.score;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la lógica de score. Sin Spring, sin base de datos.
 * Cada test verifica un comportamiento específico del algoritmo.
 */
class ClimbScoreCalculatorTest {

    // Condiciones perfectas de referencia
    private static final double TEMP_OPT  = 10.0;  // óptimo 5-12°C
    private static final double HUM_OPT   = 38.0;  // óptimo 30-45%
    private static final double WIND_OPT  = 15.0;  // óptimo 10-20 km/h
    private static final double NO_RAIN   = 0.0;
    private static final double NO_PROB   = 5.0;
    private static final double CLOUD_50  = 50.0;
    private static final double NO_RECENT = 0.0;

    @Test
    void perfectConditionsGiveHighScore() {
        int score = ClimbScoreCalculator.calculate(
                TEMP_OPT, HUM_OPT, WIND_OPT,
                NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT,
                null, null);
        // temp=30 + hum=30 + wind=20 + precip=20 = 100
        assertTrue(score >= 95, "Condiciones perfectas deberían dar score >= 95, fue: " + score);
    }

    @Test
    void heavyRainCapsScoreAt8() {
        int score = ClimbScoreCalculator.calculate(
                TEMP_OPT, HUM_OPT, WIND_OPT,
                5.0, 90.0, CLOUD_50, NO_RECENT,
                null, null);
        assertTrue(score <= 8, "Con 5mm de lluvia el score debería ser <= 8, fue: " + score);
    }

    @Test
    void highHumidityReducesScore() {
        int scoreOpt  = ClimbScoreCalculator.calculate(TEMP_OPT, 38, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null, null);
        int scoreHigh = ClimbScoreCalculator.calculate(TEMP_OPT, 85, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null, null);
        assertTrue(scoreHigh < scoreOpt, "Humedad alta debería dar menor score");
    }

    @Test
    void recentRainReducesScore() {
        int scoreDry = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 0.0, null, null);
        int scoreWet = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 8.0, null, null);
        assertTrue(scoreWet < scoreDry, "Lluvia reciente debería reducir el score");
    }

    @Test
    void sandstoneHasLowerCapThanGranite() {
        // Con lluvia reciente moderada, arenisca seca mucho más lento que granito
        int scoreGranite   = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 3.0, null, "Granito");
        int scoreSandstone = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 3.0, null, "Arenisca");
        assertTrue(scoreSandstone < scoreGranite,
                "Arenisca mojada debería tener menor score que granito mojado");
    }

    @Test
    void highDewPointCapsScore() {
        int scoreNoDew  = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null,  null);
        int scoreHighDew = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, 22.0, null);
        assertTrue(scoreHighDew <= 32, "Punto de rocío >= 20°C debería capear en 32, fue: " + scoreHighDew);
    }

    @Test
    void scoreIsAlwaysBetween1And100() {
        // Condiciones extremas
        int scoreMin = ClimbScoreCalculator.calculate(-20, 100, 100, 10, 100, 100, 20, 25.0, "Arenisca");
        int scoreMax = ClimbScoreCalculator.calculate( 10,  38,  15,  0,   0,  50,  0, null, null);
        assertTrue(scoreMin >= 1,   "Score mínimo debe ser >= 1");
        assertTrue(scoreMax <= 100, "Score máximo debe ser <= 100");
    }

    @Test
    void labelMatchesScore() {
        assertEquals("Excelente", ClimbScoreCalculator.label(90));
        assertEquals("Muy bueno", ClimbScoreCalculator.label(72));
        assertEquals("Bueno",     ClimbScoreCalculator.label(60));
        assertEquals("Regular",   ClimbScoreCalculator.label(42));
        assertEquals("Malo",      ClimbScoreCalculator.label(28));
        assertEquals("Pésimo",    ClimbScoreCalculator.label(15));
    }

    @Test
    void rockDryingProfileMatchesJS() {
        // Verificar que los perfiles coinciden con score-core.js
        assertEquals(12,   RockDryingProfile.forRockType("Granito").lookbackHours());
        assertEquals(1.30, RockDryingProfile.forRockType("Granito").capMult());
        assertEquals(72,   RockDryingProfile.forRockType("Arenisca").lookbackHours());
        assertEquals(0.45, RockDryingProfile.forRockType("Arenisca").capMult());
        assertEquals(48,   RockDryingProfile.forRockType("Conglomerado").lookbackHours());
        assertEquals(18,   RockDryingProfile.forRockType("Caliza").lookbackHours());
        assertEquals(1.0,  RockDryingProfile.forRockType(null).capMult());
    }
}
