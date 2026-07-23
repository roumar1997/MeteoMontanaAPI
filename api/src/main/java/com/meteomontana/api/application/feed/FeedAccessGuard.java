package com.meteomontana.api.application.feed;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFollowRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * LA regla de privacidad y bloqueos del feed, en UN solo sitio.
 *
 * Requisito duro (FEED_DESIGN.md): en Explorar solo autores públicos; los
 * privados solo los ven sus seguidores ACEPTADOS (o ellos mismos); el
 * bloqueador nunca ve contenido de sus bloqueados. Antes esta regla estaba
 * repartida por FeedService y DUPLICADA a mano en ShareController.
 */
@Service
public class FeedAccessGuard {

    private final SpringDataUserBlockRepository blocks;
    private final SpringDataFollowRepository follows;
    private final UserRepository users;

    public FeedAccessGuard(SpringDataUserBlockRepository blocks,
                           SpringDataFollowRepository follows,
                           UserRepository users) {
        this.blocks = blocks;
        this.follows = follows;
        this.users = users;
    }

    /** Uids que {@code uid} ha bloqueado (para filtrar páginas/comentarios). */
    public Set<String> blockedUids(String uid) {
        return blocks.findByBlockerUid(uid).stream()
                .map(UserBlockJpaEntity::getBlockedUid).collect(Collectors.toSet());
    }

    /** ¿{@code uid} ha bloqueado a {@code targetUid}? */
    public boolean hasBlocked(String uid, String targetUid) {
        return blocks.findByBlockerUid(uid).stream()
                .anyMatch(b -> b.getBlockedUid().equals(targetUid));
    }

    /** ¿{@code follower} sigue a {@code followed} con follow ACEPTADO? */
    public boolean isAcceptedFollower(String follower, String followed) {
        return follows.findById_FollowerUidAndId_FollowedUid(follower, followed)
                .map(f -> "ACCEPTED".equals(f.getStatus()))
                .orElse(false);
    }

    /**
     * ¿Puede {@code callerUid} ver el contenido del autor {@code authorUid}?
     * Sí si es él mismo, o si el autor existe y es público, o si le sigue con
     * follow aceptado. (El bloqueo se comprueba aparte con {@link #hasBlocked}:
     * cada endpoint decide si responde vacío o 404.)
     */
    public boolean canSeeUserContent(String callerUid, String authorUid) {
        if (authorUid.equals(callerUid)) return true;
        User author = users.findByUid(authorUid).orElse(null);
        if (author == null) return false;
        return author.isPublic() || isAcceptedFollower(callerUid, authorUid);
    }

    /** Versión pública sin caller (landings /s/p): solo autores públicos. */
    public boolean isAuthorPublic(String authorUid) {
        return users.findByUid(authorUid).map(User::isPublic).orElse(false);
    }
}
