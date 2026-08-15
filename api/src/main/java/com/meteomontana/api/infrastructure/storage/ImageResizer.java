package com.meteomontana.api.infrastructure.storage;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Reduce el peso de las fotos antes de guardarlas. Las cámaras de móvil suben
 * 1-1,5 MB por foto y eso son 8 s de espera con mala cobertura, que es
 * justo donde se usa la app (en la roca). A 1600 px de lado mayor y calidad 82
 * una foto de topo baja a 200-400 KB y en pantalla se ve igual.
 *
 * Dos cuidados que no son opcionales:
 *  - ORIENTACIÓN: al re-codificar se pierde el EXIF, así que el giro que
 *    indicaba se aplica a los píxeles; si no, las fotos verticales salen
 *    tumbadas.
 *  - NUNCA romper la subida: si algo falla (formato raro, imagen corrupta,
 *    memoria), se devuelven los bytes originales y la foto se guarda tal cual.
 *
 * Deliberadamente NO conserva el resto del EXIF (GPS incluido): la ubicación se
 * lee en el móvil antes de subir, y quitarla del fichero público evita publicar
 * las coordenadas exactas de quien hizo la foto.
 */
public final class ImageResizer {

    private static final Logger log = LoggerFactory.getLogger(ImageResizer.class);

    /** Lado mayor de una foto de piedra/nota/quedada. */
    public static final int MAX_PHOTO_SIDE = 1600;
    /** Lado mayor de un avatar (se muestra pequeño y redondo). */
    public static final int MAX_AVATAR_SIDE = 512;

    /** 75 es donde deja de notarse a simple vista y con zoom en un topo;
     *  medido sobre fotos reales: 1,09 MB → 339 KB (3,3× menos). */
    private static final float JPEG_QUALITY = 0.75f;

    private ImageResizer() {}

    /**
     * @return los bytes reducidos, o los ORIGINALES si no se pudo procesar o si
     *         reducir no compensaba (el resultado no era más pequeño).
     */
    public static byte[] shrink(byte[] original, int maxSide) {
        if (original == null || original.length == 0) return original;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(original));
            if (img == null) {
                log.debug("ImageIO no supo leer la imagen ({} bytes); se guarda tal cual", original.length);
                return original;
            }
            BufferedImage upright = applyExifOrientation(img, original);
            BufferedImage scaled = scaleToFit(upright, maxSide);
            byte[] out = toJpeg(scaled);
            // Si "reducir" engorda (p.ej. ya venía muy comprimida), no compensa.
            return out.length < original.length ? out : original;
        } catch (Exception | OutOfMemoryError e) {
            log.warn("No se pudo reducir la imagen ({} bytes): {}. Se guarda el original.",
                    original.length, e.toString());
            return original;
        }
    }

    // ────────────────────────────────────────────────────────────── orientación

    /** Gira/voltea según la etiqueta EXIF de orientación. Sin EXIF → tal cual. */
    private static BufferedImage applyExifOrientation(BufferedImage img, byte[] source) {
        int orientation = readExifOrientation(source);
        if (orientation <= 1) return img;

        int w = img.getWidth(), h = img.getHeight();
        // 5..8 giran 90°, así que el lienzo cambia de proporciones.
        boolean swap = orientation >= 5;
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);

        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.scale(-1, 1); t.translate(-w, 0); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.scale(1, -1); t.translate(0, -h); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 8 -> { t.translate(0, w); t.rotate(-Math.PI / 2); }
            default -> { return img; }
        }

        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, t, null);
        g.dispose();
        return out;
    }

    /** Etiqueta EXIF de orientación (1..8), o 1 si no la hay o no se pudo leer. */
    private static int readExifOrientation(byte[] source) {
        try {
            var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(source));
            var dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir == null || !dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) return 1;
            return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (Exception e) {
            return 1;
        }
    }

    // ─────────────────────────────────────────────────────────────── escalado

    /** Escala proporcionalmente hasta que el lado mayor quepa en [maxSide]. */
    private static BufferedImage scaleToFit(BufferedImage img, int maxSide) {
        int w = img.getWidth(), h = img.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxSide) return toRgb(img);

        double factor = (double) maxSide / longest;
        int nw = Math.max(1, (int) Math.round(w * factor));
        int nh = Math.max(1, (int) Math.round(h * factor));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    /** JPEG no admite transparencia: se aplana sobre blanco (PNG con alfa). */
    private static BufferedImage toRgb(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    // ───────────────────────────────────────────────────────────────── salida

    private static byte[] toJpeg(BufferedImage img) throws java.io.IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (var ios = new MemoryCacheImageOutputStream(bos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(img, null, null), param);
            }
            return bos.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
