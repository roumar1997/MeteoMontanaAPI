package com.meteomontana.api.application.radar;

import com.meteomontana.api.infrastructure.radar.RadarSites;

import java.awt.image.BufferedImage;

/**
 * Compuesto "España" (Península + Baleares): cose los círculos de los
 * radares regionales en un único lienzo equirectangular, como la vista
 * "Península y Baleares" de la web de AEMET. Donde dos radares se solapan
 * gana el eco más intenso. Canarias queda fuera del lienzo (su radar 'ca'
 * sigue disponible por separado).
 */
public final class RadarComposite {

    // Marco geográfico del lienzo (equirectangular). Cubre el ALCANCE COMPLETO
    // de los radares (240 km desde cada antena): se ve la lluvia sobre el mar,
    // Portugal y el sur de Francia — "lo que viene", como en la web de AEMET.
    public static final double NORTH = 45.7, SOUTH = 34.3, WEST = -11.6, EAST = 5.8;
    // ~2 km/px: suficiente para la vista país (el dato original es 1 km/px).
    public static final int WIDTH = 744, HEIGHT = 636;

    /** Código virtual del compuesto en la API. */
    public static final String CODE = "es";

    public static double[] bounds() {
        return new double[]{NORTH, WEST, SOUTH, EAST};
    }

    /** Vuelca un círculo regional ya repintado sobre el lienzo común. */
    public static void paste(BufferedImage canvas, BufferedImage regional, String radarCode) {
        double[] b = RadarSites.bounds(radarCode);
        if (b == null) return;
        double rNorth = b[0], rWest = b[1], rSouth = b[2], rEast = b[3];
        int plot = RadarSites.PLOT_SIZE;
        for (int y = 0; y < plot; y++) {
            double lat = rNorth - (y + 0.5) / plot * (rNorth - rSouth);
            int cy = (int) ((NORTH - lat) / (NORTH - SOUTH) * HEIGHT);
            if (cy < 0 || cy >= HEIGHT) continue;
            for (int x = 0; x < plot; x++) {
                int argb = regional.getRGB(x, y);
                int inten = RadarCumbreRenderer.intensity(argb);
                if (inten == 0) continue;
                double lon = rWest + (x + 0.5) / plot * (rEast - rWest);
                int cx = (int) ((lon - WEST) / (EAST - WEST) * WIDTH);
                if (cx < 0 || cx >= WIDTH) continue;
                if (inten > RadarCumbreRenderer.intensity(canvas.getRGB(cx, cy))) {
                    canvas.setRGB(cx, cy, argb);
                }
            }
        }
    }

    private RadarComposite() {}
}
