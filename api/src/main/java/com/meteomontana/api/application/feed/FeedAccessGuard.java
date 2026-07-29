package com.meteomontana.api.application.feed;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.UserBlockRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * LA regla de privacidad y bloqueos del feed, en UN solo sitio.
 *
 * Requisito duro (FEED_DESIGN.md): en Explorar solo autores públicos; los
 * privados solo los ven sus seguidores ACEPTADOS (o ellos mismos); el
 * bloqueador nunca ve contenido de sus bloqueados. Antes esta regla estaba
 * repartida por FeedService y DUPLICADA a mano en ShareController.
 */
@Service
@RequiredArgsConstructor
public class FeedAccessGuard {

    private final UserBlockRepository blocks;
    private final FollowRepository follows;
    private final UserRepository users;

    /** Uids que {@code uid} ha bloqueado (para filtrar páginas/comentarios). */
    public Set<String> blockedUids(String uid) {
        return blocks.blockedUidsOf(uid);
    }

    /** ¿{@code uid} ha bloqueado a {@code targetUid}? */
    public boolean hasBlocked(String uid, String targetUid) {
        return blocks.isBlocked(uid, targetUid);
    }

    /** ¿{@code follower} sigue a {@code followed} con follow ACEPTADO? */
    public boolean isAcceptedFollower(String follower, String followed) {
        return follows.isFollowing(follower, followed);
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
