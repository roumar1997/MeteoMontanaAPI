package com.meteomontana.api.domain.score;

import java.util.List;

/**
 * Estima la temperatura de la SUPERFICIE de la roca hora a hora — la memoria
 * térmica que el score no tenía: si toda la tarde hizo 35° al sol en granito,
 * al perder el sol la roca sigue caliente un buen rato (y al revés: en
 * invierno por la mañana la roca sigue helada aunque el aire ya esté bien).
 *
 * Modelo: retardo exponencial de primer orden hacia una temperatura efectiva.
 *
 *   T_efectiva(h) = T_aire(h) + gananciaSolar(h)
 *   T_roca(h)     = T_roca(h-1) + (T_efectiva(h) - T_roca(h-1)) · (1 - e^(-1/τ_ef))
 *
 *  - gananciaSolar: la radiación de onda corta (W/m²) calienta la superficie
 *    por encima del aire; a pleno sol (~900 W/m²) una roca al sol se pone
 *    fácilmente 7-8° por encima del aire. Escala lineal capada.
 *  - τ_ef = τ_roca / (1 + viento/30): el viento acelera el intercambio por
 *    convección (30 km/h ≈ mitad de retardo).
 *
 * Clase de DOMINIO pura (sin frameworks) y estática: entra la serie horaria,
 * sale la serie de temperatura de roca. Si no hay datos de radiación (caché
 * antigua sin el campo), degrada con elegancia a "roca = aire" (comportamiento
 * previo, nunca peor).
 */
public final class RockTemperatureModel {

    /** °C de sobrecalentamiento por W/m² de radiación (cap en MAX_SOLAR_GAIN). */
    private static final double SOLAR_GAIN_PER_WM2 = 1.0 / 120.0;
    private static final double MAX_SOLAR_GAIN_C = 8.0;
    /** km/h de viento que reducen τ a la mitad. */
    private static final double WIND_HALVING_KMH = 30.0;

    private RockTemperatureModel() {}

    /**
     * Serie horaria de temperatura de roca alineada con la de aire.
     *
     * @param airTemp   temperatura del aire (°C), obligatoria
     * @param radiation radiación de onda corta (W/m²); null o corta → sin sol
     * @param windSpeed viento (km/h); null o corta → sin viento
     * @param tauHours  constante de tiempo de la roca (RockThermalProfile)
     */
    public static double[] estimate(List<Double> airTemp, List<Double> radiation,
                                    List<Double> windSpeed, double tauHours) {
        int n = airTemp == null ? 0 : airTemp.size();
        double[] rock = new double[n];
        if (n == 0) return rock;

        // Arranque: la serie empieza a las 00:00 locales (sin sol, tras horas
        // de noche) → la roca ya está prácticamente a la del aire. El pequeño
        // error inicial decae solo en las primeras ~τ horas de la serie.
        rock[0] = effective(airTemp, radiation, 0);
        for (int i = 1; i < n; i++) {
            double target = effective(airTemp, radiation, i);
            double wind = at(windSpeed, i, 0.0);
            double tauEf = Math.max(0.25, tauHours / (1.0 + wind / WIND_HALVING_KMH));
            double alpha = 1.0 - Math.exp(-1.0 / tauEf);
            rock[i] = rock[i - 1] + (target - rock[i - 1]) * alpha;
        }
        return rock;
    }

    /** Aire + ganancia solar de esa hora (0 de noche o sin dato de radiación). */
    private static double effective(List<Double> airTemp, List<Double> radiation, int i) {
        double air = at(airTemp, i, 0.0);
        double rad = at(radiation, i, 0.0);
        return air + Math.min(MAX_SOLAR_GAIN_C, Math.max(0, rad) * SOLAR_GAIN_PER_WM2);
    }

    private static double at(List<Double> list, int i, double fallback) {
        if (list == null || i >= list.size() || list.get(i) == null) return fallback;
        return list.get(i);
    }
}
