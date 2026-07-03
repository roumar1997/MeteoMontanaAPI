package com.meteomontana.api.infrastructure.radar;

import java.util.Map;

/**
 * Georreferenciación de los 15 radares regionales de AEMET.
 *
 * Coordenadas de antena de la base de datos OPERA de EUMETNET (la red
 * europea oficial de radares). El PPI regional es un círculo de 240 km de
 * radio centrado en la antena, dibujado a 1 km/píxel en un lienzo de
 * 480x480 (el GIF de AEMET añade 50 px de pie con la leyenda: 480x530).
 *
 * Las esquinas geográficas salen de proyectar ese círculo como cuadrado
 * equirectangular (suficiente a esta escala; el ajuste fino se valida
 * visualmente contra los bordes de provincia en el mapa de la app).
 */
public final class RadarSites {

    public record Site(String code, String name, double lat, double lon) {}

    /** Radio del PPI regional en km (= píxeles del lienzo / 2). */
    public static final double RANGE_KM = 240.0;
    /** Lado del área de dibujo del GIF (el resto es pie de leyenda). */
    public static final int PLOT_SIZE = 480;

    public static final Map<String, Site> SITES = Map.ofEntries(
            Map.entry("am", new Site("am", "Almería",      36.8324,  -2.0821)),
            Map.entry("sa", new Site("sa", "Asturias",     43.4625,  -6.3019)),
            Map.entry("pm", new Site("pm", "Illes Balears",39.3797,   2.7851)),
            Map.entry("ba", new Site("ba", "Barcelona",    41.4081,   1.8848)),
            Map.entry("cc", new Site("cc", "Cáceres",      39.4288,  -6.2853)),
            Map.entry("co", new Site("co", "A Coruña",     43.16903, -8.5269)),
            Map.entry("ma", new Site("ma", "Madrid",       40.1759,  -3.7136)),
            Map.entry("ml", new Site("ml", "Málaga",       36.6133,  -4.6593)),
            Map.entry("mu", new Site("mu", "Murcia",       38.26438, -1.1897)),
            Map.entry("vd", new Site("vd", "Palencia",     41.9959,  -4.6021)),
            Map.entry("ca", new Site("ca", "Las Palmas",   28.0186, -15.6144)),
            Map.entry("se", new Site("se", "Sevilla",      37.68868, -6.33308)),
            Map.entry("va", new Site("va", "Valencia",     39.1761,  -0.2521)),
            Map.entry("ss", new Site("ss", "Vizcaya",      43.4033,  -2.8419)),
            Map.entry("za", new Site("za", "Zaragoza",     41.7339,  -0.5459)));

    private static final double KM_PER_DEG_LAT = 111.32;

    /** [latNorte, lonOeste, latSur, lonEste] del cuadrado que envuelve el PPI. */
    public static double[] bounds(String code) {
        Site s = SITES.get(code);
        if (s == null) return null;
        double dLat = RANGE_KM / KM_PER_DEG_LAT;
        double dLon = RANGE_KM / (KM_PER_DEG_LAT * Math.cos(Math.toRadians(s.lat())));
        return new double[]{s.lat() + dLat, s.lon() - dLon, s.lat() - dLat, s.lon() + dLon};
    }

    /** Radar más cercano a un punto (para que la app no lleve la tabla). */
    public static Site nearest(double lat, double lon) {
        Site best = null;
        double bestD = Double.MAX_VALUE;
        for (Site s : SITES.values()) {
            double dLat = (s.lat() - lat) * KM_PER_DEG_LAT;
            double dLon = (s.lon() - lon) * KM_PER_DEG_LAT * Math.cos(Math.toRadians(lat));
            double d = dLat * dLat + dLon * dLon;
            if (d < bestD) { bestD = d; best = s; }
        }
        return best;
    }

    private RadarSites() {}
}
