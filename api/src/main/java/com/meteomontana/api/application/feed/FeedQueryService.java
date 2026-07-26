package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.feed.FeedViews.FeedPostView;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.port.FeedPostRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LECTURA del feed: página por scope, actividad de un usuario y post único.
 * Solo lecturas (readOnly); la privacidad y los bloqueos los decide
 * {@link FeedAccessGuard} y el mapeo a vistas {@link FeedViewMapper}.
 */
@Service
public class FeedQueryService {

    private static final int MAX_PAGE = 50;

    private final FeedPostRepository posts;
    private final FollowRepository follows;
    private final FeedAccessGuard guard;
    private final FeedViewMapper viewMapper;

    public FeedQueryService(FeedPostRepository posts,
                            FollowRepository follows,
                            FeedAccessGuard guard,
                            FeedViewMapper viewMapper) {
        this.posts = posts;
        this.follows = follows;
        this.guard = guard;
        this.viewMapper = viewMapper;
    }

    /**
     * Página del feed, más recientes primero. Cursor keyset: {@code before} es
     * el id del último post de la página anterior (null en la primera).
     */
    @Transactional(readOnly = true)
    public List<FeedPostView> page(String uid, String scope, Long before, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE));
        long cursor = before == null ? Long.MAX_VALUE : before;

        List<FeedPost> page;
        if ("following".equalsIgnoreCase(scope)) {
            // Seguidos ACEPTADOS + yo mismo (mis posts también salen en mi feed).
            List<String> authors = new ArrayList<>(follows.followingOf(uid));
            authors.add(uid);
            page = posts.pageByAuthors(authors, cursor, capped);
        } else if ("mine".equalsIgnoreCase(scope)) {
            // Solo mis posts (pestaña "mi actividad").
            page = posts.pageByAuthors(List.of(uid), cursor, capped);
        } else {
            page = posts.pageAllPublic(cursor, capped);
        }

        // El bloqueador no ve el contenido de sus bloqueados. Filtro post-query:
        // la página puede quedar algo corta, pero el cursor sigue avanzando.
        Set<String> blocked = guard.blockedUids(uid);
        page = page.stream().filter(p -> !blocked.contains(p.getUserUid())).toList();
        if (page.isEmpty()) return List.of();

        return viewMapper.mapViews(uid, page);
    }

    /**
     * Posts de UN usuario (sección "actividad" de su perfil público). Mismas
     * reglas de privacidad que {@link #single}, pero devolviendo lista VACÍA
     * en vez de 404 (es una sección del perfil, no un recurso).
     */
    @Transactional(readOnly = true)
    public List<FeedPostView> pageOfUser(String uid, String targetUid, Long before, int limit) {
        if (targetUid == null || targetUid.isBlank()) return List.of();
        int capped = Math.max(1, Math.min(limit, MAX_PAGE));
        long cursor = before == null ? Long.MAX_VALUE : before;

        if (!targetUid.equals(uid)) {
            if (guard.hasBlocked(uid, targetUid)) return List.of();
            if (!guard.canSeeUserContent(uid, targetUid)) return List.of();
        }
        List<FeedPost> page = posts.pageByAuthors(List.of(targetUid), cursor, capped);
        if (page.isEmpty()) return List.of();
        return viewMapper.mapViews(uid, page);
    }

    /**
     * UN post por id (lo usa la app al tocar una notificación). 404 si no
     * existe, si el caller bloqueó al autor, o si el autor es privado y el
     * caller no es él ni un seguidor aceptado (no filtramos "me bloquearon"
     * ni exponemos que el post existe: siempre 404, nunca 403).
     */
    @Transactional(readOnly = true)
    public FeedPostView single(String uid, long postId) {
        FeedPost p = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("post no encontrado"));

        String authorUid = p.getUserUid();
        if (!authorUid.equals(uid)) {
            if (guard.hasBlocked(uid, authorUid)) {
                throw new NotFoundException("post no encontrado");
            }
            if (!guard.canSeeUserContent(uid, authorUid)) {
                throw new NotFoundException("post no encontrado");
            }
        }

        List<FeedPostView> views = viewMapper.mapViews(uid, List.of(p));
        if (views.isEmpty()) { // cuenta del autor borrada
            throw new NotFoundException("post no encontrado");
        }
        return views.get(0);
    }
}
