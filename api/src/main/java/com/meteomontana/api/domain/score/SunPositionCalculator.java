package com.meteomontana.api.domain.score;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Posición del sol (azimut y altura) y "¿le da el sol a esta pared?" —
 * matemática pura, sin APIs. Algoritmo solar estándar (NOAA simplificado),
 * precisión ~0.5°: de sobra para sol/sombra de una pared.
 *
 * Convenio: azimut 0° = Norte, 90° = Este, 180° = Sur, 270° = Oeste.
 * Los rumbos de pared son los 8 puntos que vota la comunidad (N..NO, con
 * O = Oeste en español). Una pared "mira" hacia su rumbo: recibe sol cuando
 * el sol está a menos de {@link #SUN_HALF_ANGLE_DEG} de su normal y por
 * encima de {@link #MIN_ELEVATION_DEG} (sin relieve; fase 2 = horizonte DEM).
 */
public final class SunPositionCalculator {

    /** Media apertura: la pared coge sol hasta ±78° de su normal (rasante cuenta poco pero cuenta). */
    public static final double SUN_HALF_ANGLE_DEG = 78.0;
    /** Sol por debajo de ~8° apenas calienta una pared (árboles, relieve cercano). */
    public static final double MIN_ELEVATION_DEG = 8.0;

    private SunPositionCalculator() {}

    /** Azimut del sol en grados (0=N, horario), para hora LOCAL con offset UTC en segundos. */
    public static double azimuthDeg(LocalDateTime localTime, double lat, double lon, int utcOffsetSeconds) {
        double[] azEl = solarPosition(localTime, lat, lon, utcOffsetSeconds);
        return azEl[0];
    }

    /** Altura del sol sobre el horizonte en grados (negativa de noche). */
    public static double elevationDeg(LocalDateTime localTime, double lat, double lon, int utcOffsetSeconds) {
        double[] azEl = solarPosition(localTime, lat, lon, utcOffsetSeconds);
        return azEl[1];
    }

    /** ¿Recibe sol directo una pared orientada a `aspect` (N..NO) en ese momento? */
    public static boolean isWallInSun(String aspect, LocalDateTime localTime,
                                      double lat, double lon, int utcOffsetSeconds) {
        Double wallAz = aspectToDegrees(aspect);
        if (wallAz == null) return false;
        double[] azEl = solarPosition(localTime, lat, lon, utcOffsetSeconds);
        if (azEl[1] < MIN_ELEVATION_DEG) return false;
        double diff = Math.abs(((azEl[0] - wallAz) % 360 + 540) % 360 - 180);
        return diff <= SUN_HALF_ANGLE_DEG;
    }

    /** Rumbo español (O = Oeste) → grados. Null si no es un rumbo válido. */
    public static Double aspectToDegrees(String aspect) {
        if (aspect == null) return null;
        return switch (aspect.trim().toUpperCase()) {
            case "N" -> 0.0; case "NE" -> 45.0; case "E" -> 90.0; case "SE" -> 135.0;
            case "S" -> 180.0; case "SO" -> 225.0; case "O" -> 270.0; case "NO" -> 315.0;
            default -> null;
        };
    }

    /** [azimutDeg (0=N, horario), alturaDeg]. NOAA simplificado. */
    private static double[] solarPosition(LocalDateTime local, double lat, double lon, int utcOffsetSeconds) {
        LocalDate d = local.toLocalDate();
        double hourUtc = local.getHour() + local.getMinute() / 60.0 - utcOffsetSeconds / 3600.0;
        int n = d.getDayOfYear();

        // Declinación solar (Cooper) y ecuación del tiempo (Spencer), en grados/minutos.
        double gamma = 2 * Math.PI / 365.0 * (n - 1 + (hourUtc - 12) / 24.0);
        double eqTimeMin = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma));
        double declRad = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma);

        double timeOffsetMin = eqTimeMin + 4 * lon;                 // lon Este positiva
        double trueSolarMin = hourUtc * 60 + timeOffsetMin;
        double hourAngleDeg = trueSolarMin / 4.0 - 180.0;

        double latRad = Math.toRadians(lat);
        double haRad = Math.toRadians(hourAngleDeg);
        double cosZen = Math.sin(latRad) * Math.sin(declRad)
                + Math.cos(latRad) * Math.cos(declRad) * Math.cos(haRad);
        cosZen = Math.max(-1, Math.min(1, cosZen));
        double zenRad = Math.acos(cosZen);
        double elevation = 90.0 - Math.toDegrees(zenRad);

        double sinZen = Math.sin(zenRad);
        double az;
        if (sinZen < 1e-6) {
            az = 180.0;
        } else {
            double cosAz = (Math.sin(latRad) * cosZen - Math.sin(declRad)) / (Math.cos(latRad) * sinZen);
            cosAz = Math.max(-1, Math.min(1, cosAz));
            az = Math.toDegrees(Math.acos(cosAz));
            // acos da 0..180 desde el Sur; el signo del ángulo horario decide E/O.
            az = hourAngleDeg > 0 ? 180.0 + az : 180.0 - az;
        }
        return new double[]{(az % 360 + 360) % 360, elevation};
    }
}
