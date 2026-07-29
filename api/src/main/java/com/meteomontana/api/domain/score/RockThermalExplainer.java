package com.meteomontana.api.domain.score;

/**
 * Traduce la memoria térmica de la roca ({@link RockTemperatureModel}) a un
 * factor legible del acordeón «¿Por qué este índice?»: qué roca es, si sigue
 * caliente y cuánto tarda en enfriarse. Regla de dominio pura (sin frameworks).
 */
public final class RockThermalExplainer {

    /** Mismo shape que ForecastResponse.ScoreFactor, pero en el dominio. */
    public record Explanation(String name, String display, boolean passes) {}

    /** Por encima de esto la roca resta agarre por sí misma. */
    private static final double WARM_ROCK_C = 26.0;
    /** Diferencial roca-aire que delata que aún guarda el sol. */
    private static final double HOLDING_HEAT_DELTA_C = 3.0;

    private RockThermalExplainer() {}

    public static Explanation explain(String rockType, double rockTempC,
                                      double airTempC, double tauHours) {
        // Nombre CORTO fijo («ROCA»): el largo «ROCA · GRANITO» chocaba con la
        // descripción en la fila de factores de Android. El tipo va al texto.
        String rock = rockType == null || rockType.isBlank()
                ? "Roca" : capitalize(rockType.trim());
        String lag = "~" + Math.max(1, Math.round(tauHours)) + " h";
        long shown = Math.round(rockTempC);

        boolean warm = rockTempC >= WARM_ROCK_C
                || (rockTempC - airTempC) >= HOLDING_HEAT_DELTA_C;
        if (warm) {
            return new Explanation("ROCA",
                    rock + " aún templado (" + shown + "°): guarda el calor " + lag + " tras el sol",
                    false);
        }
        if (rockTempC <= 22.0) {
            return new Explanation("ROCA",
                    rock + " frío (" + shown + "°): buena fricción · se enfría en " + lag,
                    true);
        }
        return new Explanation("ROCA",
                rock + " templado (" + shown + "°) · inercia " + lag,
                true);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s
                : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
