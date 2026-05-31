package com.meteomontana.api.domain.score;

/**
 * Calcula el score de condiciones de escalada (1-100).
 * Traducción directa de js/score-core.js → climbScore().
 * Lógica pura — sin dependencias de framework, testeable con JUnit.
 */
public class ClimbScoreCalculator {

    // ── Tablas de lookup [lo, hi, pts] — primer match gana ──

    private static final double[][] TEMP_TABLE = {
        {  5,    12,   30},
        {  4,     5,   29}, { 12,    13,   29},
        {  3,     4,   28}, { 13,    14,   27},
        {  2,     3,   26}, { 14,    15,   25},
        {  1,     2,   24}, { 15,    16,   22},
        {  0,     1,   22}, { 16,    17,   19},
        { -1,     0,   20}, { 17,    18,   16},
        { -2,    -1,   18}, { 18,    19,   13},
        { -3,    -2,   16}, { 19,    20,   10},
        { -5,    -3,   13}, { 20,    21,    8},
        { -8,    -5,   10}, { 21,    22,    6},
        {-10,    -8,    6}, { 22,    23,    4},
                            { 23,    24,    3},
        {Double.NEGATIVE_INFINITY, -10, 3}, { 24, 25, 2},
                            { 25,    27,    1},
    };

    private static final double[][] HUM_TABLE = {
        { 30,    45,   30},
        { 25,    30,   28}, { 45,    50,   28},
        { 20,    25,   25}, { 50,    55,   25},
        { 15,    20,   21}, { 55,    60,   22},
        {Double.NEGATIVE_INFINITY, 15, 16}, { 60, 65, 18},
        { 65,    70,   13},
        { 70,    75,    9},
        { 75,    80,    5},
        { 80,    85,    3},
        { 85,    90,    1},
    };

    private static final double[][] WIND_TABLE = {
        { 10,    20,   20},
        {  8,    10,   19}, { 20,    22,   19},
        {  6,     8,   17}, { 22,    25,   17},
        {  4,     6,   14}, { 25,    30,   13},
        {  2,     4,   11}, { 30,    35,    9},
        {Double.NEGATIVE_INFINITY, 2, 8}, { 35, 40, 6},
        { 40,    50,    3},
        { 50,    60,    1},
    };

    /**
     * Score horario de condiciones de escalada (1-100).
     *
     * @param tempMax     Temperatura (°C)
     * @param humidity    Humedad relativa (%)
     * @param windMax     Viento (km/h)
     * @param precipSum   Precipitación (mm)
     * @param precipProb  Probabilidad de lluvia (%)
     * @param cloudCover  Nubosidad (%; 50 si desconocido)
     * @param recentRain  Lluvia acumulada en las N horas previas (mm)
     * @param dewPoint    Punto de rocío (°C; null si no disponible)
     * @param rockType    Tipo de roca (null = genérico)
     */
    public static int calculate(
            double tempMax, double humidity, double windMax,
            double precipSum, double precipProb,
            double cloudCover, double recentRain,
            Double dewPoint, String rockType) {

        double score = lookup(tempMax, TEMP_TABLE)
                     + lookup(humidity, HUM_TABLE)
                     + lookup(windMax, WIND_TABLE)
                     + precipScore(precipSum, precipProb);

        // ── CAP por lluvia actual ──
        if      (precipSum >= 3.0)  score = Math.min(score,  8);
        else if (precipSum >= 1.5)  score = Math.min(score, 14);
        else if (precipSum >= 0.7)  score = Math.min(score, 20);
        else if (precipSum >= 0.3)  score = Math.min(score, 28);
        else if (precipSum >= 0.1)  score = Math.min(score, 35);
        else if (precipSum >  0.0)  score = Math.min(score, 45);

        // ── CAP por probabilidad alta (sin lluvia real) ──
        if (precipSum == 0) {
            if      (precipProb >= 90) score = Math.min(score, 55);
            else if (precipProb >= 75) score = Math.min(score, 68);
            else if (precipProb >= 60) score = Math.min(score, 78);
        }

        // ── CAP por lluvia reciente ──
        if (recentRain > 0) {
            int dryBoost = (windMax >= 20 ? 1 : 0) + (tempMax >= 18 ? 1 : 0);
            double rainCap;
            if      (recentRain > 10)   rainCap = 15;
            else if (recentRain > 6)    rainCap = 22;
            else if (recentRain > 3)    rainCap = 32;
            else if (recentRain > 1.5)  rainCap = 42;
            else if (recentRain > 0.5)  rainCap = 45;
            else if (recentRain > 0.2)  rainCap = 55;
            else                        rainCap = 68;

            RockDryingProfile profile = RockDryingProfile.forRockType(rockType);
            rainCap = Math.min(95, Math.round(rainCap * profile.capMult()));
            rainCap += dryBoost * 3;
            score = Math.min(score, rainCap);
        }

        // ── CAP por punto de rocío alto ──
        if (dewPoint != null) {
            if      (dewPoint >= 20) score = Math.min(score, 32);
            else if (dewPoint >= 17) score = Math.min(score, 48);
            else if (dewPoint >= 15) score = Math.min(score, 62);
            else if (dewPoint >= 12) score = Math.min(score, 76);
            else if (dewPoint >= 10) score = Math.min(score, 88);
            if      (dewPoint <= 0)  score += 3;
            else if (dewPoint <= 5)  score += 2;
        }

        // ── Ajustes por sol/nubes ──
        if      (tempMax >= 22 && cloudCover < 30) score -= 8;
        else if (tempMax >= 25 && cloudCover < 50) score -= 5;
        if      (tempMax <  8  && cloudCover < 40) score += 3;

        return (int) Math.round(Math.min(100, Math.max(1, score)));
    }

    // ── Helpers privados ──

    private static double lookup(double val, double[][] table) {
        for (double[] row : table) {
            if (val >= row[0] && val <= row[1]) return row[2];
        }
        return 0;
    }

    private static double precipScore(double sum, double prob) {
        if (sum == 0 && prob <  10) return 20;
        if (sum == 0 && prob <  20) return 18;
        if (sum == 0 && prob <  30) return 16;
        if (sum == 0 && prob <  40) return 13;
        if (sum == 0 && prob <  55) return 10;
        if (sum == 0 && prob <  70) return  7;
        if (sum == 0 && prob <  85) return  4;
        if (sum == 0)               return  2;
        if (sum <= 0.1)             return  3;
        if (sum <= 0.3)             return  2;
        if (sum <= 0.7)             return  1;
        return 0;
    }

    /** Etiqueta textual del score (equivalente a climbScoreLabel() en JS) */
    public static String label(int score) {
        if (score >= 85) return "Excelente";
        if (score >= 70) return "Muy bueno";
        if (score >= 55) return "Bueno";
        if (score >= 40) return "Regular";
        if (score >= 25) return "Malo";
        return "Pésimo";
    }
}
