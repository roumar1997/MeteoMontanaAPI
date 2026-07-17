package com.meteomontana.api.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataFeedPostRepository extends JpaRepository<FeedPostJpaEntity, Long> {

    /**
     * Scope TODOS: solo autores con perfil público, evaluado en CADA lectura
     * (si un perfil pasa a privado, sus posts desaparecen de TODOS al momento).
     * Cursor keyset por id descendente: pasar Long.MAX_VALUE en la 1ª página.
     */
    @Query(value = """
            SELECT p.* FROM feed_posts p
            JOIN users u ON u.uid = p.user_uid
            WHERE u.is_public = TRUE AND p.id < :before
            ORDER BY p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FeedPostJpaEntity> pageAllPublic(@Param("before") long before, @Param("limit") int limit);

    /** Scope SIGUIENDO: posts de los uids dados (seguidos aceptados + yo). */
    @Query(value = """
            SELECT p.* FROM feed_posts p
            WHERE p.user_uid IN (:uids) AND p.id < :before
            ORDER BY p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FeedPostJpaEntity> pageByAuthors(@Param("uids") List<String> uids,
                                          @Param("before") long before,
                                          @Param("limit") int limit);

    /** Para publicar idempotente: mismo usuario + misma vía + mismo tipo. */
    Optional<FeedPostJpaEntity> findByUserUidAndLineIdAndKind(String userUid, String lineId, String kind);

    /** Posts de esos tipos (NEW_BLOCK/NEW_LINE) creados desde {@code since}
     *  — para la historia "novedades de la semana". */
    List<FeedPostJpaEntity> findByKindInAndCreatedAtAfterOrderByCreatedAtDesc(
            java.util.Collection<String> kinds, java.time.LocalDateTime since);
}
