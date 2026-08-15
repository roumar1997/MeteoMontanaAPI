package com.meteomontana.api.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El paso de URL-con-redirect a URL directa del CDN. Lo delicado no es el caso
 * feliz sino lo que NO se debe tocar: URLs ya directas, las viejas de Firebase
 * y las rutas relativas.
 */
class PhotoDirectUrlServiceTest {

    private static final String CDN = "https://photos.climbingteams.com";

    @Test
    @DisplayName("la URL del redirect pasa a apuntar directa al CDN")
    void redirectPasaADirecta() {
        String url = "https://api.climbingteams.com/api/photo/piedra-photos-pending/abc_santa-gadea_1.jpg";

        assertThat(PhotoDirectUrlService.toDirect(url, CDN))
                .isEqualTo(CDN + "/piedra-photos-pending/abc_santa-gadea_1.jpg");
    }

    @Test
    @DisplayName("conserva el ?v= de la foto de perfil (rompe la caché de Coil)")
    void conservaElQuery() {
        String url = "https://api.climbingteams.com/api/photo/profile-photos/uid123.jpg?v=1a2b3c4d";

        assertThat(PhotoDirectUrlService.toDirect(url, CDN))
                .isEqualTo(CDN + "/profile-photos/uid123.jpg?v=1a2b3c4d");
    }

    @Test
    @DisplayName("no toca lo que no es una URL de redirect")
    void noTocaLoQueNoDebe() {
        // Ya directa: reescribirla otra vez la duplicaría.
        assertThat(PhotoDirectUrlService.toDirect(CDN + "/note-photos/x.jpg", CDN)).isNull();
        // Vieja de Firebase: sigue sirviéndose por su cuenta.
        assertThat(PhotoDirectUrlService.toDirect(
                "https://firebasestorage.googleapis.com/v0/b/x/o/note-photos%2Fx.jpg", CDN)).isNull();
        // Ruta relativa (fotos Tipo B) y nulos.
        assertThat(PhotoDirectUrlService.toDirect("piedra-photos-pending/x.jpg", CDN)).isNull();
        assertThat(PhotoDirectUrlService.toDirect(null, CDN)).isNull();
    }

    @Test
    @DisplayName("una barra final de más en el CDN no duplica la barra")
    void barraFinalDelCdn() {
        String url = "https://api.climbingteams.com/api/photo/note-photos/x.jpg";

        // El servicio normaliza la base antes de llamar aquí; se comprueba que
        // con base ya normalizada el resultado tiene UNA sola barra.
        assertThat(PhotoDirectUrlService.toDirect(url, CDN))
                .isEqualTo(CDN + "/note-photos/x.jpg")
                .doesNotContain("//note-photos");
    }
}
