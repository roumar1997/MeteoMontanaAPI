package com.meteomontana.api.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Validación de que un fichero subido es REALMENTE una imagen, mirando sus
 * primeros bytes ("magic bytes"), no solo el Content-Type que declara el
 * cliente (que es falsificable). Defensa barata contra subir un ejecutable o
 * un fichero arbitrario disfrazado de imagen.
 *
 * Firmas soportadas (las que la app envía): JPEG, PNG, WebP, GIF.
 */
public final class ImageValidation {

    private ImageValidation() {}

    /**
     * @throws IllegalArgumentException si los bytes no corresponden a una imagen
     *         de un formato soportado.
     */
    public static void ensureRealImage(MultipartFile file) throws IOException {
        byte[] head = readHead(file, 12);
        if (!isJpeg(head) && !isPng(head) && !isWebp(head) && !isGif(head)) {
            throw new IllegalArgumentException("File content is not a valid image");
        }
    }

    private static byte[] readHead(MultipartFile file, int n) throws IOException {
        try (var in = file.getInputStream()) {
            byte[] buf = new byte[n];
            int read = in.readNBytes(buf, 0, n);
            if (read < buf.length) {
                byte[] trimmed = new byte[read];
                System.arraycopy(buf, 0, trimmed, 0, read);
                return trimmed;
            }
            return buf;
        }
    }

    // FF D8 FF
    private static boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    // 89 50 4E 47 0D 0A 1A 0A
    private static boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    // "RIFF"...."WEBP"
    private static boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    // "GIF87a" o "GIF89a"
    private static boolean isGif(byte[] b) {
        return b.length >= 6
                && b[0] == 'G' && b[1] == 'I' && b[2] == 'F'
                && b[3] == '8' && (b[4] == '7' || b[4] == '9') && b[5] == 'a';
    }
}
