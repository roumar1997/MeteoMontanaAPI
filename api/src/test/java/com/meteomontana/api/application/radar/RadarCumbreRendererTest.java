package com.meteomontana.api.application.radar;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renderer probado contra un GIF REAL del radar de Madrid (2026-07-03). */
class RadarCumbreRendererTest {

    private static final Set<Integer> CUMBRE_BLUES = Set.of(
            0xFF5C8FD6, 0xFF3D6FBF, 0xFF274F98);

    @Test
    void repintaSoloAzulesCumbreYFondoTransparente() throws Exception {
        byte[] gif = getClass().getResourceAsStream("/radar/ma-sample.gif").readAllBytes();

        byte[] png = new RadarCumbreRenderer().render(gif, List.of());

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(img);
        assertEquals(480, img.getWidth());
        assertEquals(480, img.getHeight());

        int transparent = 0, painted = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0) { transparent++; continue; }
                painted++;
                assertTrue(CUMBRE_BLUES.contains(argb),
                        "píxel no-Cumbre en (" + x + "," + y + "): "
                                + Integer.toHexString(argb));
            }
        }
        // El GIF de muestra era un día casi seco: casi todo transparente,
        // pero algo de eco débil había (los puntitos cian al oeste de Madrid).
        assertTrue(painted > 0, "no se pintó ninguna lluvia");
        assertTrue(transparent > painted * 10, "demasiado píxel pintado: decorado sin filtrar");
    }
}
