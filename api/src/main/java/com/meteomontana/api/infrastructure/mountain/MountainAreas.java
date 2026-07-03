package com.meteomontana.api.infrastructure.mountain;

import java.util.List;

/**
 * Las 9 áreas del boletín de montaña de AEMET, con un centro y radio
 * aproximados para decidir qué escuelas "caen" en cada macizo.
 *
 * El radio es deliberadamente generoso: el boletín describe la meteo del
 * macizo entero, así que una escuela a 50 km de la cumbre sigue siendo
 * usuaria legítima del boletín ("¿habrá tormenta en la sierra?").
 */
public final class MountainAreas {

    public record Area(String code, String name, double lat, double lon, double radiusKm) {}

    public static final List<Area> AREAS = List.of(
            new Area("peu1", "Picos de Europa",            43.19, -4.85, 60),
            new Area("nav1", "Pirineo Navarro",            42.95, -1.10, 60),
            new Area("arn1", "Pirineo Aragonés",           42.65, -0.30, 80),
            new Area("cat1", "Pirineo Catalán",            42.45,  1.70, 90),
            new Area("rio1", "Ibérica Riojana",            42.05, -2.70, 60),
            new Area("arn2", "Ibérica Aragonesa",          40.40, -1.40, 90),
            new Area("mad2", "Guadarrama y Somosierra",    40.85, -3.90, 60),
            new Area("gre1", "Sierra de Gredos",           40.25, -5.30, 60),
            new Area("nev1", "Sierra Nevada",              37.05, -3.30, 60));

    private static final double KM_PER_DEG = 111.32;

    /** Área cuyo radio contiene el punto (la más cercana si varias). Null si ninguna. */
    public static Area forLocation(double lat, double lon) {
        Area best = null;
        double bestKm = Double.MAX_VALUE;
        for (Area a : AREAS) {
            double dLat = (a.lat() - lat) * KM_PER_DEG;
            double dLon = (a.lon() - lon) * KM_PER_DEG * Math.cos(Math.toRadians(lat));
            double km = Math.sqrt(dLat * dLat + dLon * dLon);
            if (km <= a.radiusKm() && km < bestKm) { bestKm = km; best = a; }
        }
        return best;
    }

    private MountainAreas() {}
}
