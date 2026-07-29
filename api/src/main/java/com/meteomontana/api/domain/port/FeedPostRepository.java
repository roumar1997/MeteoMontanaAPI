package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.FeedPost;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistencia de posts del feed (puerto de dominio, sin JPA). */
public interface FeedPostRepository {

    /**
     * Scope TODOS: solo autores con perfil público, evaluado en CADA lectura.
     * Cursor keyset por id descendente: pasar Long.MAX_VALUE en la 1ª página.
     */
    List<FeedPost> pageAllPublic(long before, int limit);

    /** Scope SIGUIENDO / MÍOS: posts de los uids dados, mismo cursor keyset. */
    List<FeedPost> pageByAuthors(List<String> authorUids, long before, int limit);

    Optional<FeedPost> findById(long id);

    boolean existsById(long id);

    /** Para publicar idempotente: mismo usuario + misma vía + mismo tipo. */
    Optional<FeedPost> findByUserLineAndKind(String userUid, String lineId, String kind);

    /** Posts de esos tipos (NEW_BLOCK/NEW_LINE) desde {@code since}, recientes
     *  primero — para la historia "novedades de la semana". */
    List<FeedPost> findRecentByKinds(Collection<String> kinds, LocalDateTime since);

    /** Crea la fila y devuelve el post con id y createdAt asignados. */
    FeedPost create(FeedPost post);

    /** Actualiza SOLO la ruta de la foto de celebración (subida/reemplazo). */
    void updatePhotoPath(long postId, String photoPath);

    /** Borra el post (likes y comentarios caen por ON DELETE CASCADE). */
    void deleteById(long postId);
}
