package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFollowRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Feed social (pestaña Comunidad). Ver FEED_DESIGN.md en el repo Android.
 *
 * Privacidad (requisito duro): en el scope TODOS solo salen autores con
 * perfil público, evaluado en cada lectura. En SIGUIENDO solo salen los
 * seguidos con follow ACEPTADO (los privados solo los ven sus seguidores).
 * Bloqueados: el bloqueador no ve posts/comentarios de sus bloqueados
 * (mismo patrón que notas y line_comments).
 */
@Service
public class FeedService {

    public static final String KIND_TICK = "TICK";
    public static final String KIND_PROJECT_DONE = "PROJECT_DONE";
    /** Reservados: los crea el backend al aprobar contribuciones (fase 3). */
    public static final String KIND_NEW_BLOCK = "NEW_BLOCK";
    public static final String KIND_NEW_LINE = "NEW_LINE";

    private static final int MAX_PAGE = 50;

    public record FeedAuthor(String uid, String username, String displayName, String photoUrl) {}

    public record FeedPostView(
            long id, String kind, LocalDateTime createdAt, FeedAuthor author,
            String schoolId, String schoolName,
            String blockId, String blockName,
            String lineId, String lineName, String grade,
            String photoPath, String linePath,
            long likeCount, boolean likedByMe, long commentCount, boolean mine) {}

    public record FeedCommentView(String id, long postId, String uid, String author,
                                  String text, LocalDateTime createdAt, boolean mine) {}

    private final SpringDataFeedPostRepository posts;
    private final SpringDataFeedLikeRepository likes;
    private final SpringDataFeedCommentRepository comments;
    private final SpringDataFollowRepository follows;
    private final SpringDataUserBlockRepository blocks;
    private final SpringDataSchoolBlockRepository schoolBlocks;
    private final SpringDataSchoolRepository schools;
    private final UserRepository users;
    private final UserDtoMapper mapper;
    private final UserModerationService moderation;

    public FeedService(SpringDataFeedPostRepository posts,
                       SpringDataFeedLikeRepository likes,
                       SpringDataFeedCommentRepository comments,
                       SpringDataFollowRepository follows,
                       SpringDataUserBlockRepository blocks,
                       SpringDataSchoolBlockRepository schoolBlocks,
                       SpringDataSchoolRepository schools,
                       UserRepository users,
                       UserDtoMapper mapper,
                       UserModerationService moderation) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.follows = follows;
        this.blocks = blocks;
        this.schoolBlocks = schoolBlocks;
        this.schools = schools;
        this.users = users;
        this.mapper = mapper;
        this.moderation = moderation;
    }

    // ------------------------------------------------------------ lectura

    /**
     * Página del feed, más recientes primero. Cursor keyset: {@code before} es
     * el id del último post de la página anterior (null en la primera).
     */
    @Transactional(readOnly = true)
    public List<FeedPostView> page(String uid, String scope, Long before, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE));
        long cursor = before == null ? Long.MAX_VALUE : before;

        List<FeedPostJpaEntity> page;
        if ("following".equalsIgnoreCase(scope)) {
            // Seguidos ACEPTADOS + yo mismo (mis posts también salen en mi feed).
            List<String> authors = new ArrayList<>(follows.findFollowingOf(uid));
            authors.add(uid);
            page = posts.pageByAuthors(authors, cursor, capped);
        } else {
            page = posts.pageAllPublic(cursor, capped);
        }

        // El bloqueador no ve el contenido de sus bloqueados. Filtro post-query:
        // la página puede quedar algo corta, pero el cursor sigue avanzando.
        Set<String> blocked = blocks.findByBlockerUid(uid).stream()
                .map(UserBlockJpaEntity::getBlockedUid).collect(Collectors.toSet());
        page = page.stream().filter(p -> !blocked.contains(p.getUserUid())).toList();
        if (page.isEmpty()) return List.of();

        List<Long> ids = page.stream().map(FeedPostJpaEntity::getId).toList();

        Map<Long, Long> likeCounts = toCountMap(likes.countByPostIds(ids));
        Map<Long, Long> commentCounts = toCountMap(comments.countByPostIds(ids));
        Set<Long> mine = Set.copyOf(likes.likedPostIds(uid, ids));

        Map<String, FeedAuthor> authors = loadAuthors(
                page.stream().map(FeedPostJpaEntity::getUserUid).distinct().toList());

        // Foto y trazo se leen EN VIVO de block_lines (si se re-dibuja el topo,
        // el feed lo refleja). Una query por página, no por post.
        Map<String, SchoolBlockJpaEntity> blocksById = schoolBlocks
                .findAllById(page.stream().map(FeedPostJpaEntity::getBlockId).distinct().toList())
                .stream().collect(Collectors.toMap(SchoolBlockJpaEntity::getId, Function.identity()));

        return page.stream().map(p -> {
            FeedAuthor author = authors.get(p.getUserUid());
            if (author == null) return null; // cuenta borrada → fuera del feed

            String photoPath = null;
            String linePath = null;
            SchoolBlockJpaEntity block = blocksById.get(p.getBlockId());
            if (block != null) {
                photoPath = block.getPhotoPath();
                if (p.getLineId() != null) {
                    BlockLineJpaEntity line = block.getLines().stream()
                            .filter(l -> p.getLineId().equals(l.getId()))
                            .findFirst().orElse(null);
                    if (line != null) {
                        linePath = line.getLinePath();
                        if (line.getPhotoPath() != null) photoPath = line.getPhotoPath();
                    }
                }
            }
            return new FeedPostView(p.getId(), p.getKind(), p.getCreatedAt(), author,
                    p.getSchoolId(), p.getSchoolName(),
                    p.getBlockId(), p.getBlockName(),
                    p.getLineId(), p.getLineName(), p.getGrade(),
                    photoPath, linePath,
                    likeCounts.getOrDefault(p.getId(), 0L),
                    mine.contains(p.getId()),
                    commentCounts.getOrDefault(p.getId(), 0L),
                    p.getUserUid().equals(uid));
        }).filter(v -> v != null).toList();
    }

    // ------------------------------------------------------------ publicar

    /**
     * Publica un ascenso propio (TICK / PROJECT_DONE). Idempotente: repetir la
     * misma vía+tipo devuelve el post existente en vez de duplicarlo.
     * NEW_BLOCK/NEW_LINE no se aceptan desde el cliente (los creará el backend
     * al aprobar contribuciones).
     */
    @Transactional
    public long publish(String uid, String blockId, String lineId, String kind) {
        moderation.ensureCanPost(uid);
        if (!KIND_TICK.equals(kind) && !KIND_PROJECT_DONE.equals(kind)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "kind debe ser TICK o PROJECT_DONE");
        }
        SchoolBlockJpaEntity block = schoolBlocks.findById(blockId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "piedra no encontrada"));

        BlockLineJpaEntity line = null;
        if (lineId != null && !lineId.isBlank()) {
            line = block.getLines().stream()
                    .filter(l -> lineId.equals(l.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "la vía no pertenece a esa piedra"));
            var existing = posts.findByUserUidAndLineIdAndKind(uid, lineId, kind);
            if (existing.isPresent()) return existing.get().getId();
        }

        String schoolName = block.getSchoolId() == null ? null
                : schools.findById(block.getSchoolId())
                        .map(s -> s.getName()).orElse(null);

        FeedPostJpaEntity saved = posts.save(new FeedPostJpaEntity(
                uid, block.getSchoolId(), schoolName,
                block.getId(), block.getName(),
                line != null ? line.getId() : null,
                line != null ? line.getName() : null,
                line != null ? line.getGrade() : null,
                kind));
        return saved.getId();
    }

    /** Borra un post propio (o cualquiera si es admin). */
    @Transactional
    public void delete(String uid, long postId, boolean isAdmin) {
        FeedPostJpaEntity p = posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado"));
        if (!isAdmin && !p.getUserUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "solo puedes borrar tus posts");
        }
        posts.delete(p); // likes y comentarios caen por ON DELETE CASCADE
    }

    // ------------------------------------------------------------ likes

    /** Da like (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long like(String uid, long postId) {
        if (!posts.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
        }
        if (!likes.existsById(new FeedLikeJpaEntity.Key(postId, uid))) {
            likes.save(new FeedLikeJpaEntity(postId, uid));
        }
        return countLikes(postId);
    }

    /** Quita el like (idempotente). Devuelve el contador resultante. */
    @Transactional
    public long unlike(String uid, long postId) {
        FeedLikeJpaEntity.Key key = new FeedLikeJpaEntity.Key(postId, uid);
        if (likes.existsById(key)) likes.deleteById(key);
        return countLikes(postId);
    }

    // ------------------------------------------------------------ comentarios

    @Transactional(readOnly = true)
    public List<FeedCommentView> listComments(String uid, long postId) {
        Set<String> blocked = blocks.findByBlockerUid(uid).stream()
                .map(UserBlockJpaEntity::getBlockedUid).collect(Collectors.toSet());
        return comments.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .filter(c -> !blocked.contains(c.getUid()))
                .map(c -> new FeedCommentView(c.getId(), c.getPostId(), c.getUid(),
                        c.getAuthor(), c.getText(), c.getCreatedAt(), c.getUid().equals(uid)))
                .toList();
    }

    @Transactional
    public FeedCommentView addComment(String uid, long postId, String text) {
        moderation.ensureCanPost(uid);
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text is required");
        }
        if (!posts.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
        }
        String trimmed = text.trim();
        if (trimmed.length() > 1000) trimmed = trimmed.substring(0, 1000);

        String author = users.findByUid(uid)
                .map(u -> u.getUsername() != null ? "@" + u.getUsername()
                        : (u.getDisplayName() != null ? u.getDisplayName() : "Anónimo"))
                .orElse("Anónimo");

        FeedCommentJpaEntity saved = comments.save(new FeedCommentJpaEntity(
                UUID.randomUUID().toString(), postId, uid, author, trimmed));
        return new FeedCommentView(saved.getId(), saved.getPostId(), saved.getUid(),
                saved.getAuthor(), saved.getText(), saved.getCreatedAt(), true);
    }

    /** Borra un comentario propio (o cualquiera si es admin). */
    @Transactional
    public void deleteComment(String uid, String commentId, boolean isAdmin) {
        FeedCommentJpaEntity c = comments.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comentario no encontrado"));
        if (!isAdmin && !c.getUid().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "solo puedes borrar tus comentarios");
        }
        comments.delete(c);
    }

    // ------------------------------------------------------------ helpers

    private long countLikes(long postId) {
        Map<Long, Long> counts = toCountMap(likes.countByPostIds(List.of(postId)));
        return counts.getOrDefault(postId, 0L);
    }

    private Map<String, FeedAuthor> loadAuthors(List<String> uids) {
        Map<String, FeedAuthor> out = new HashMap<>();
        for (User u : users.findByUids(uids)) {
            // Misma regla que el ranking: perfil privado → vista "locked"
            // (username/foto sí, resto no).
            var profile = u.isPublic() ? mapper.toPublic(u) : mapper.toPublicLocked(u);
            out.put(u.getUid(), new FeedAuthor(u.getUid(), profile.username(),
                    profile.displayName(), profile.photoUrl()));
        }
        return out;
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> out = new HashMap<>();
        for (Object[] r : rows) {
            out.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }
        return out;
    }
}
