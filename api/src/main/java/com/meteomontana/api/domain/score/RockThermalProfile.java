package com.meteomontana.api.domain.score;

/**
 * Perfil térmico de cada tipo de roca: cuánto tarda en soltar (o coger) el
 * calor. Espejo del patrón de {@link RockDryingProfile}.
 *
 * tauHours = constante de tiempo del retardo exponencial SIN viento: con
 * τ = 3 h, tras un cambio brusco del aire la roca ha recorrido ~63% de la
 * diferencia en 3 h y ~95% en 9 h. El viento acorta τ (convección) — eso lo
 * aplica {@link RockTemperatureModel}, no este perfil.
 *
 * Valores por efusividad térmica (densidad × calor específico × conductividad)
 * y masa típica de la formación:
 *  - Granito/cuarcita/basalto: densos y conductores → almacenan mucho y lo
 *    sueltan lento (el "horno" de Zarzalejo al atardecer).
 *  - Caliza/dolomía/conglomerado: intermedias.
 *  - Arenisca: porosa (el aire de los poros aísla) → superficie que cambia rápido.
 *  - Pizarra: láminas finas, poca masa térmica → casi sigue al aire.
 */
public record RockThermalProfile(double tauHours) {

    public static RockThermalProfile forRockType(String rockType) {
        String key = rockType == null ? "" : rockType.toLowerCase().trim();

        if (key.contains("granito"))      return new RockThermalProfile(3.0);
        if (key.contains("cuarcita"))     return new RockThermalProfile(3.0);
        if (key.contains("basalto"))      return new RockThermalProfile(3.5);
        if (key.contains("volc"))         return new RockThermalProfile(3.5);
        if (key.contains("conglomerado")) return new RockThermalProfile(2.0);
        if (key.contains("arenisca"))     return new RockThermalProfile(1.2);
        if (key.contains("pizarra"))      return new RockThermalProfile(0.8);

        // caliza, dolomía y cualquier otro tipo → perfil intermedio
        return new RockThermalProfile(1.8);
    }
}
