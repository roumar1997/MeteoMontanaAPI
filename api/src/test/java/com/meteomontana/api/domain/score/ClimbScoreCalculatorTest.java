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
        System.out.println("[perfectConditions] temp=10°C hum=38% viento=15km/h sin lluvia → score: "
                + score + " (" + ClimbScoreCalculator.label(score) + ")");
        assertTrue(score >= 95, "Condiciones perfectas deberían dar score >= 95, fue: " + score);
    }

    @Test
    void heavyRainCapsScoreAt8() {
        int score = ClimbScoreCalculator.calculate(
                TEMP_OPT, HUM_OPT, WIND_OPT,
                5.0, 90.0, CLOUD_50, NO_RECENT,
                null, null);
        System.out.println("[heavyRain] 5mm de lluvia + 90% prob → score: "
                + score + " (" + ClimbScoreCalculator.label(score) + ") — cap esperado: <=8");
        assertTrue(score <= 8, "Con 5mm de lluvia el score debería ser <= 8, fue: " + score);
    }

    @Test
    void highHumidityReducesScore() {
        int scoreOpt  = ClimbScoreCalculator.calculate(TEMP_OPT, 38,  WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null, null);
        int scoreHigh = ClimbScoreCalculator.calculate(TEMP_OPT, 85, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null, null);
        System.out.println("[humidity] hum=38% → " + scoreOpt + " (" + ClimbScoreCalculator.label(scoreOpt) + ")"
                + "  |  hum=85% → " + scoreHigh + " (" + ClimbScoreCalculator.label(scoreHigh) + ")");
        assertTrue(scoreHigh < scoreOpt, "Humedad alta debería dar menor score");
    }

    @Test
    void recentRainReducesScore() {
        int scoreDry = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 0.0, null, null);
        int scoreWet = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 8.0, null, null);
        System.out.println("[recentRain] sin lluvia reciente → " + scoreDry + " (" + ClimbScoreCalculator.label(scoreDry) + ")"
                + "  |  8mm lluvia reciente → " + scoreWet + " (" + ClimbScoreCalculator.label(scoreWet) + ")");
        assertTrue(scoreWet < scoreDry, "Lluvia reciente debería reducir el score");
    }

    @Test
    void sandstoneHasLowerCapThanGranite() {
        int scoreGranite   = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 3.0, null, "Granito");
        int scoreSandstone = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, 3.0, null, "Arenisca");
        System.out.println("[rockType] mismas condiciones, 3mm lluvia reciente:"
                + "  Granito → " + scoreGranite + " (" + ClimbScoreCalculator.label(scoreGranite) + ")"
                + "  |  Arenisca → " + scoreSandstone + " (" + ClimbScoreCalculator.label(scoreSandstone) + ")");
        assertTrue(scoreSandstone < scoreGranite,
                "Arenisca mojada debería tener menor score que granito mojado");
    }

    @Test
    void highDewPointCapsScore() {
        int scoreNoDew   = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, null,  null);
        int scoreHighDew = ClimbScoreCalculator.calculate(TEMP_OPT, HUM_OPT, WIND_OPT, NO_RAIN, NO_PROB, CLOUD_50, NO_RECENT, 22.0, null);
        System.out.println("[dewPoint] sin rocío → " + scoreNoDew + " (" + ClimbScoreCalculator.label(scoreNoDew) + ")"
                + "  |  rocío=22°C → " + scoreHighDew + " (" + ClimbScoreCalculator.label(scoreHighDew) + ") — cap esperado: <=32");
        assertTrue(scoreHighDew <= 32, "Punto de rocío >= 20°C debería capear en 32, fue: " + scoreHighDew);
    }

    @Test
    void scoreIsAlwaysBetween1And100() {
        int scoreMin = ClimbScoreCalculator.calculate(-20, 100, 100, 10, 100, 100, 20, 25.0, "Arenisca");
        int scoreMax = ClimbScoreCalculator.calculate( 10,  38,  15,  0,   0,  50,  0, null, null);
        System.out.println("[bounds] condiciones pésimas → " + scoreMin + " (" + ClimbScoreCalculator.label(scoreMin) + ")"
                + "  |  condiciones perfectas → " + scoreMax + " (" + ClimbScoreCalculator.label(scoreMax) + ")");
        assertTrue(scoreMin >= 1,   "Score mínimo debe ser >= 1");
        assertTrue(scoreMax <= 100, "Score máximo debe ser <= 100");
    }

    @Test
    void labelMatchesScore() {
        System.out.println("[labels]"
                + "  90→" + ClimbScoreCalculator.label(90)
                + "  72→" + ClimbScoreCalculator.label(72)
                + "  60→" + ClimbScoreCalculator.label(60)
                + "  42→" + ClimbScoreCalculator.label(42)
                + "  28→" + ClimbScoreCalculator.label(28)
                + "  15→" + ClimbScoreCalculator.label(15));
        assertEquals("Excelente", ClimbScoreCalculator.label(90));
        assertEquals("Muy bueno", ClimbScoreCalculator.label(72));
        assertEquals("Bueno",     ClimbScoreCalculator.label(60));
        assertEquals("Regular",   ClimbScoreCalculator.label(42));
        assertEquals("Malo",      ClimbScoreCalculator.label(28));
        assertEquals("Pésimo",    ClimbScoreCalculator.label(15));
    }

    @Test
    void rockDryingProfileMatchesJS() {
        RockDryingProfile granito      = RockDryingProfile.forRockType("Granito");
        RockDryingProfile arenisca     = RockDryingProfile.forRockType("Arenisca");
        RockDryingProfile conglomerado = RockDryingProfile.forRockType("Conglomerado");
        RockDryingProfile caliza       = RockDryingProfile.forRockType("Caliza");
        RockDryingProfile defaultRock  = RockDryingProfile.forRockType(null);
        System.out.println("[rockProfiles]"
                + "  Granito: "      + granito.lookbackHours()      + "h x" + granito.capMult()
                + "  Arenisca: "     + arenisca.lookbackHours()     + "h x" + arenisca.capMult()
                + "  Conglomerado: " + conglomerado.lookbackHours() + "h x" + conglomerado.capMult()
                + "  Caliza: "       + caliza.lookbackHours()       + "h x" + caliza.capMult()
                + "  Default: "      + defaultRock.lookbackHours()  + "h x" + defaultRock.capMult());
        assertEquals(12,   granito.lookbackHours());
        assertEquals(1.30, granito.capMult());
        assertEquals(72,   arenisca.lookbackHours());
        assertEquals(0.45, arenisca.capMult());
        assertEquals(48,   conglomerado.lookbackHours());
        assertEquals(18,   caliza.lookbackHours());
        assertEquals(1.0,  defaultRock.capMult());
    }
}
