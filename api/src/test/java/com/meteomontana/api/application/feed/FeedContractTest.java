package com.meteomontana.api.application.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meteomontana.api.application.feed.FeedViews.FeedAuthor;
import com.meteomontana.api.application.feed.FeedViews.FeedCommentView;
import com.meteomontana.api.application.feed.FeedViews.FeedLineView;
import com.meteomontana.api.application.feed.FeedViews.FeedPostView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CONTRATO del feed con las apps (Android + iOS vía shared KMP).
 *
 * Las apps parsean las respuestas de /api/feed con los DTOs de
 * shared/src/commonMain/.../FeedDto.kt (kotlinx.serialization). Un cambio de
 * FORMA aquí (renombrar un campo, cambiar objeto por string, cambiar un tipo)
 * rompe el parseo EN SILENCIO en producción: la lista sale vacía y los POST
 * "no hacen nada" (pasó el 2026-07-14: author pasó de objeto a String y nadie
 * pudo comentar).
 *
 * Este test serializa las vistas reales con la misma config Jackson que usa
 * Spring Boot y clava la forma del JSON. Si se pone rojo:
 *   - ¿Cambio ADITIVO (campo nuevo nullable)? → añadirlo aquí y al DTO de las
 *     apps (FeedDto.kt + espejo del test FeedContractTest.kt en el repo
 *     Android). Las apps viejas lo ignoran (ignoreUnknownKeys): OK.
 *   - ¿Cambio de forma/renombrado/borrado? → PARA. Eso rompe las apps ya
 *     instaladas. Hay que hacerlo aditivo (campo nuevo conviviendo) o no
 *     hacerlo. Regla expand-contract.
 */
class FeedContractTest {

    /** Misma config que Spring Boot: fechas ISO-8601, no timestamps. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static FeedPostView samplePost() {
        return new FeedPostView(
                42L, "TICK", LocalDateTime.of(2026, 7, 19, 10, 30, 0),
                new FeedAuthor("uid123", "ana_escaladora", "Ana", "https://x/foto.jpg"),
                "school-1", "Zarzalejo",
                "block-9", "Piedra 15",
                "line-7", "La ola", "7a",
                "BOULDER", "Granito",
                "topos/p15.jpg", "[{\"x\":0.1,\"y\":0.9}]",
                3L, true, 5L, false,
                "SIT", "¡Por fin!", "https://cdn/celebracion.jpg",
                List.of(new FeedLineView("La ola", "7a", "SIT", "[{\"x\":0.1,\"y\":0.9}]")));
    }

    private static FeedCommentView sampleComment() {
        return new FeedCommentView(
                "77", 42L, "uid456",
                new FeedAuthor("uid456", "karly", "Karly", null),
                "Qué máquina", LocalDateTime.of(2026, 7, 19, 11, 0, 0),
                false, 2L, true, "55");
    }

    @Test
    void postSerializaConLaFormaQueEsperanLasApps() throws Exception {
        JsonNode n = MAPPER.readTree(MAPPER.writeValueAsString(samplePost()));

        // Campos obligatorios del DTO de las apps (sin default) — si faltan o
        // cambian de tipo, el parseo kotlinx explota.
        assertTrue(n.get("id").isNumber(), "id debe ser numérico");
        assertTrue(n.get("kind").isTextual(), "kind debe ser String");
        assertTrue(n.get("createdAt").isTextual(), "createdAt debe ser String ISO");

        // EL BUG DE 2026-07-14: author debe ser OBJETO, jamás String.
        JsonNode author = n.get("author");
        assertNotNull(author, "author no puede faltar");
        assertTrue(author.isObject(), "author debe ser un OBJETO {uid,username,...}, no String");
        assertTrue(author.get("uid").isTextual(), "author.uid debe ser String");
        assertTrue(author.has("username"), "author.username debe existir (nullable)");
        assertTrue(author.has("displayName"), "author.displayName debe existir (nullable)");
        assertTrue(author.has("photoUrl"), "author.photoUrl debe existir (nullable)");

        // Campos opcionales que las apps leen por NOMBRE exacto.
        for (String field : new String[]{
                "schoolId", "schoolName", "blockId", "blockName",
                "lineId", "lineName", "grade", "discipline", "rockType",
                "photoPath", "linePath", "likeCount", "likedByMe",
                "commentCount", "mine", "startType", "caption", "photoUrl",
                "blockLines"}) {
            assertTrue(n.has(field), "falta el campo '" + field + "' del contrato");
        }
        assertTrue(n.get("likeCount").isNumber());
        assertTrue(n.get("likedByMe").isBoolean());
        assertTrue(n.get("blockLines").isArray(), "blockLines debe ser array");
        JsonNode line = n.get("blockLines").get(0);
        for (String field : new String[]{"name", "grade", "startType", "linePath"}) {
            assertTrue(line.has(field), "falta blockLines[]." + field);
        }
    }

    @Test
    void comentarioSerializaConLaFormaQueEsperanLasApps() throws Exception {
        JsonNode n = MAPPER.readTree(MAPPER.writeValueAsString(sampleComment()));

        assertTrue(n.get("id").isTextual(), "id de comentario debe ser String");
        assertTrue(n.get("text").isTextual(), "text debe ser String");
        assertTrue(n.get("createdAt").isTextual(), "createdAt debe ser String ISO");

        JsonNode author = n.get("author");
        assertNotNull(author, "author no puede faltar");
        assertTrue(author.isObject(), "author de comentario debe ser OBJETO (bug 2026-07-14)");

        for (String field : new String[]{
                "postId", "uid", "mine", "likeCount", "likedByMe", "parentId"}) {
            assertTrue(n.has(field), "falta el campo '" + field + "' del contrato");
        }
        assertTrue(n.get("likeCount").isNumber());
        assertTrue(n.get("parentId").isTextual());
    }

    @Test
    void fechasVanEnIsoSinZonaComoEsperanLasApps() throws Exception {
        // Las apps parsean createdAt como LocalDateTime UTC (toInstant(UTC) en
        // Android, df.timeZone=UTC en iOS). Timestamps numéricos o sufijo Z
        // romperían el parseo de horas relativas.
        JsonNode n = MAPPER.readTree(MAPPER.writeValueAsString(samplePost()));
        String createdAt = n.get("createdAt").asText();
        assertTrue(createdAt.startsWith("2026-07-19T10:30"),
                "createdAt debe ser ISO-8601 local (fue: " + createdAt + ")");
        assertFalse(createdAt.endsWith("Z"), "createdAt no debe llevar sufijo Z");
    }
}
