package com.meteomontana.api.domain.score;

/**
 * Perfil de secado de cada tipo de roca.
 * lookbackHours → cuántas horas hacia atrás mirar para calcular lluvia reciente
 * capMult       → multiplicador del cap de lluvia reciente (>1 = seca rápido, <1 = lento)
 */
public record RockDryingProfile(int lookbackHours, double capMult) {

    /** Devuelve el perfil correspondiente al tipo de roca dado. */
    public static RockDryingProfile forRockType(String rockType) {
        String key = rockType == null ? "" : rockType.toLowerCase().trim();

        if (key.contains("granito"))      return new RockDryingProfile(12, 1.30);
        if (key.contains("basalto"))      return new RockDryingProfile(12, 1.30);
        if (key.contains("cuarcita"))     return new RockDryingProfile(12, 1.30);
        if (key.contains("volc"))         return new RockDryingProfile(12, 1.30); // volcánica / volcanica
        if (key.contains("pizarra"))      return new RockDryingProfile(12, 1.30);
        if (key.contains("arenisca"))     return new RockDryingProfile(72, 0.45);
        if (key.contains("conglomerado")) return new RockDryingProfile(48, 0.65);

        // caliza y cualquier otro tipo → perfil por defecto
        return new RockDryingProfile(18, 1.0);
    }
}
