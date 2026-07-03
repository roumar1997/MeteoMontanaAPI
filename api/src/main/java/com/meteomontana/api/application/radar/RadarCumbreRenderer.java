package com.meteomontana.api.application.radar;

import com.meteomontana.api.infrastructure.radar.RadarSites;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * "Cocina Cumbre": convierte el GIF crudo de AEMET en un PNG transparente
 * con la lluvia repintada en la paleta de la app.
 *
 * 1. Recorta al área de dibujo (480x480; fuera queda el pie con la leyenda)
 *    y al círculo del PPI (fuera del alcance del radar no hay dato).
 * 2. Elimina el "decorado": fondo negro, gris exterior, y — lo importante —
 *    los píxeles ESTÁTICOS (bordes de provincia, logos, textos), que son
 *    idénticos en todos los frames mientras que la lluvia se mueve.
 *    Con menos de 3 frames aún no hay máscara fiable: se descartan además
 *    el amarillo puro y el blanco (bordes/textos) como red de seguridad.
 * 3. Clasifica el eco por la paleta dBZ de AEMET en 3 niveles y lo repinta:
 *    débil → azul claro, medio → azul medio, fuerte → azul intenso Cumbre.
 */
@Component
public class RadarCumbreRenderer {

    // Azules Cumbre subidos de saturación: a escala país los tonos apagados
    // del mockup se perdían sobre el mapa topográfico.
    private static final int WEAK   = argb(0x66, 0xA8, 0xF5);
    private static final int MEDIUM = argb(0x2B, 0x6D, 0xE3);
    private static final int STRONG = argb(0x0E, 0x3A, 0x9C);
    private static final int TRANSPARENT = 0;

    private static final int PLOT = RadarSites.PLOT_SIZE;
    private static final double RADIUS_PX = PLOT / 2.0 - 0.5;
    private static final int MIN_FRAMES_FOR_MASK = 3;

    /**
     * @param target GIF crudo del frame a pintar.
     * @param recent GIFs recientes del MISMO radar (incluido o no el target)
     *               para construir la máscara de estáticos.
     */
    public byte[] render(byte[] target, List<byte[]> recent) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(32 * 1024);
        ImageIO.write(renderImage(target, recent), "png", bos);
        return bos.toByteArray();
    }

    /** Igual que render() pero devolviendo la imagen (para el compuesto España). */
    public BufferedImage renderImage(byte[] target, List<byte[]> recent) throws IOException {
        BufferedImage src = read(target);
        List<BufferedImage> others = recent.stream()
                .map(RadarCumbreRenderer::readUnchecked)
                .filter(img -> img != null)
                .toList();
        boolean maskReady = others.size() >= MIN_FRAMES_FOR_MASK;

        BufferedImage out = new BufferedImage(PLOT, PLOT, BufferedImage.TYPE_INT_ARGB);
        double c = (PLOT - 1) / 2.0;
        for (int y = 0; y < PLOT; y++) {
            for (int x = 0; x < PLOT; x++) {
                double dx = x - c, dy = y - c;
                if (dx * dx + dy * dy > RADIUS_PX * RADIUS_PX) continue; // fuera del PPI
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                if (isBackdrop(rgb)) continue;
                if (isStatic(x, y, rgb, others, maskReady)) continue;
                out.setRGB(x, y, classify(rgb, maskReady));
            }
        }
        return out;
    }

    /**
     * Dilatación de 1 px: cada eco contagia a sus vecinos vacíos con su mismo
     * color. Las motas sueltas pasan a verse como manchas — mucho más legible
     * a escala país sin inventarse lluvia (medio píxel de radio real).
     */
    public static void dilate(java.awt.image.BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] src = img.getRGB(0, 0, w, h, null, 0, w);
        int[] out = src.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (intensity(src[y * w + x]) > 0) continue;
                int best = 0, bestColor = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int c = src[ny * w + nx];
                        int inten = intensity(c);
                        if (inten > best) { best = inten; bestColor = c; }
                    }
                }
                if (best > 0) out[y * w + x] = bestColor;
            }
        }
        img.setRGB(0, 0, w, h, out, 0, w);
    }

    /** Prioridad de intensidad para el compuesto (fuerte pisa a débil). */
    public static int intensity(int argb) {
        if (argb == STRONG) return 3;
        if (argb == MEDIUM) return 2;
        if (argb == WEAK) return 1;
        return 0;
    }

    /** Fondo/decorado por color fijo: negro, gris exterior, blanco de textos. */
    private static boolean isBackdrop(int rgb) {
        return rgb == 0x000000 || rgb == 0x7F7F7F || rgb == 0xFFFFFF;
    }

    /** Un píxel de eco idéntico en TODOS los frames recientes es decorado. */
    private static boolean isStatic(int x, int y, int rgb,
                                    List<BufferedImage> others, boolean maskReady) {
        if (!maskReady) return false;
        for (BufferedImage o : others) {
            if ((o.getRGB(x, y) & 0xFFFFFF) != rgb) return false;
        }
        return true;
    }

    /** Paleta dBZ de AEMET → 3 azules Cumbre. */
    private static int classify(int rgb, boolean maskReady) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        // Fuerte: amarillos, naranjas, rojos y magentas (>= ~36 dBZ).
        if (r >= 0xC8 && b < 0xA0) {
            // Sin máscara de estáticos todavía, el amarillo puro puede ser un
            // borde de provincia: mejor perderlo 20 min que pintar el mapa.
            if (!maskReady && rgb == 0xFFFF00) return TRANSPARENT;
            return STRONG;
        }
        // Medio: verdes y cian saturados (~24-30 dBZ).
        if ((g >= 0xC0 && r <= 0x40) || rgb == 0x438323) return MEDIUM;
        // Débil: la familia de azules (~12-18 dBZ).
        return WEAK;
    }

    private static BufferedImage read(byte[] data) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) throw new IOException("GIF ilegible");
        return img;
    }

    private static BufferedImage readUnchecked(byte[] data) {
        try { return read(data); } catch (IOException e) { return null; }
    }

    private static int argb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
