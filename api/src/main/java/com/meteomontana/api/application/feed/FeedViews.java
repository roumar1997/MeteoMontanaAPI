package com.meteomontana.api.application.feed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EL CONTRATO del feed con las apps: tipos de post y formas de las vistas.
 * Las apps parsean estos records con los DTOs de shared (FeedDto.kt) — su
 * forma la vigila FeedContractTest (backend) + FeedContractTest.kt (apps).
 * Cambios SOLO aditivos y nullable (expand-contract).
 */
public final class FeedViews {

    private FeedViews() {}

    public static final String KIND_TICK = "TICK";
    public static final String KIND_PROJECT_DONE = "PROJECT_DONE";
    /** Reservados: los crea el backend al aprobar contribuciones. */
    public static final String KIND_NEW_BLOCK = "NEW_BLOCK";
    public static final String KIND_NEW_LINE = "NEW_LINE";

    public record FeedAuthor(String uid, String username, String displayName, String photoUrl) {}

    /** Vía de la cara portada en un post NEW_BLOCK (para pintarla sobre la foto). */
    public record FeedLineView(String name, String grade, String startType, String linePath) {}

    public record FeedPostView(
            long id, String kind, LocalDateTime createdAt, FeedAuthor author,
            String schoolId, String schoolName,
            String blockId, String blockName,
            String lineId, String lineName, String grade,
            String discipline, String rockType,
            String photoPath, String linePath,
            long likeCount, boolean likedByMe, long commentCount, boolean mine,
            String startType, String caption, String photoUrl,
            // Solo en NEW_BLOCK: vías de la cara portada (null en el resto).
            // Campo ADITIVO y nullable → las apps viejas lo ignoran.
            List<FeedLineView> blockLines) {}

    // author como OBJETO (no String): las apps deserializan FeedAuthorDto —
    // mandar el snapshot "@usuario" rompía el parseo y los comentarios ni se
    // listaban ni parecían enviarse (aunque el POST sí insertaba).
    // likeCount/likedByMe/parentId (V57): likes y respuestas de comentarios.
    public record FeedCommentView(String id, long postId, String uid, FeedAuthor author,
                                  String text, LocalDateTime createdAt, boolean mine,
                                  long likeCount, boolean likedByMe, String parentId) {}
}
