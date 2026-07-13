package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

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
            String discipline, String rockType,
            String photoPath, String linePath,
            long likeCount, boolean likedByMe, long commentCount, boolean mine,
            String startType, String caption) {}

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
    private final NotificationService notifications;
    private final PushSender push;

    public FeedService(SpringDataFeedPostRepository posts,
                       SpringDataFeedLikeRepository likes,
                       SpringDataFeedCommentRepository comments,
                       SpringDataFollowRepository follows,
                       SpringDataUserBlockRepository blocks,
                       SpringDataSchoolBlockRepository schoolBlocks,
                       SpringDataSchoolRepository schools,
                       UserRepository users,
                       UserDtoMapper mapper,
                       UserModerationService moderation,
                       NotificationService notifications,
                       PushSender push) {
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
        this.notifications = notifications;
        this.push = push;
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
        } else if ("mine".equalsIgnoreCase(scope)) {
            // Solo mis posts (pestaña "mi actividad").
            page = posts.pageByAuthors(List.of(uid), cursor, capped);
        } else {
            page = posts.pageAllPublic(cursor, capped);
        }

        // El bloqueador no ve el contenido de sus bloqueados. Filtro post-query:
        // la página puede quedar algo corta, pero el cursor sigue avanzando.
        Set<String> blocked = blocks.findByBlockerUid(uid).stream()
                .map(UserBlockJpaEntity::getBlockedUid).collect(Collectors.toSet());
        page = page.stream().filter(p -> !blocked.contains(p.getUserUid())).toList();
        if (page.isEmpty()) return List.of();

        return mapViews(uid, page);
    }

    /**
     * Posts de UN usuario (sección "actividad" de su perfil público). Mismas
     * reglas de privacidad que {@link #single}, pero devolviendo lista VACÍA
     * en vez de 404 (es una sección del perfil, no un recurso):
     *  - target inexistente, o privado y el caller no es él ni seguidor
     *    ACEPTADO → vacía;
     *  - el caller bloqueó al target → vacía.
     */
    @Transactional(readOnly = true)
    public List<FeedPostView> pageOfUser(String uid, String targetUid, Long before, int limit) {
        if (targetUid == null || targetUid.isBlank()) return List.of();
        int capped = Math.max(1, Math.min(limit, MAX_PAGE));
        long cursor = before == null ? Long.MAX_VALUE : before;

        if (!targetUid.equals(uid)) {
            boolean iBlockedTarget = blocks.findByBlockerUid(uid).stream()
                    .anyMatch(b -> b.getBlockedUid().equals(targetUid));
            if (iBlockedTarget) return List.of();
            User target = users.findByUid(targetUid).orElse(null);
            if (target == null) return List.of();
            if (!target.isPublic() && !isAcceptedFollower(uid, targetUid)) return List.of();
        }
        List<FeedPostJpaEntity> page = posts.pageByAuthors(List.of(targetUid), cursor, capped);
        if (page.isEmpty()) return List.of();
        return mapViews(uid, page);
    }

    /**
     * UN post por id (lo usa la app al tocar una notificación). 404 si no
     * existe, si el caller bloqueó al autor, o si el autor es privado y el
     * caller no es él ni un seguidor aceptado (no filtramos "me bloquearon"
     * ni exponemos que el post existe: siempre 404, nunca 403).
     */
    @Transactional(readOnly = true)
    public FeedPostView single(String uid, long postId) {
        FeedPostJpaEntity p = posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado"));

        String authorUid = p.getUserUid();
        if (!authorUid.equals(uid)) {
            boolean iBlockedAuthor = blocks.findByBlockerUid(uid).stream()
                    .anyMatch(b -> b.getBlockedUid().equals(authorUid));
            if (iBlockedAuthor) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
            }
            User author = users.findByUid(authorUid).orElse(null);
            if (author == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
            }
            if (!author.isPublic() && !isAcceptedFollower(uid, authorUid)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
            }
        }

        List<FeedPostView> views = mapViews(uid, List.of(p));
        if (views.isEmpty()) { // cuenta del autor borrada
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado");
        }
        return views.get(0);
    }

    /** ¿{@code follower} sigue a {@code followed} con follow ACEPTADO? */
    private boolean isAcceptedFollower(String follower, String followed) {
        return follows.findById_FollowerUidAndId_FollowedUid(follower, followed)
                .map(f -> "ACCEPTED".equals(f.getStatus()))
                .orElse(false);
    }

    /** Mapea posts ya filtrados a vistas (contadores, autor, foto/trazo en vivo). */
    private List<FeedPostView> mapViews(String uid, List<FeedPostJpaEntity> page) {
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
            String startType = null;
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
                        if (line.getStartType() != null) startType = line.getStartType().name();
                    }
                }
            }
            return new FeedPostView(p.getId(), p.getKind(), p.getCreatedAt(), author,
                    p.getSchoolId(), p.getSchoolName(),
                    p.getBlockId(), p.getBlockName(),
                    p.getLineId(), p.getLineName(), p.getGrade(),
                    p.getDiscipline(), p.getRockType(),
                    photoPath, linePath,
                    likeCounts.getOrDefault(p.getId(), 0L),
                    mine.contains(p.getId()),
                    commentCounts.getOrDefault(p.getId(), 0L),
                    p.getUserUid().equals(uid),
                    startType, p.getCaption());
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
        return publish(uid, blockId, lineId, kind, null, null);
    }

    /**
     * @param discipline modalidad opcional enviada por el cliente (BOULDER | ROUTE).
     *        Si viene null/desconocida se deriva de la piedra (toda piedra tiene
     *        modalidad); se snapshotea en el post junto al rock_type de la escuela.
     * @param caption descripción opcional del autor: se trimea, vacía → null,
     *        truncada a 500. Si el post ya existía (idempotencia) NO se toca.
     */
    @Transactional
    public long publish(String uid, String blockId, String lineId, String kind,
                        String discipline, String caption) {
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

        FeedPostJpaEntity post = newPost(uid, block, line, kind, discipline);
        post.setCaption(normalizeCaption(caption));
        return posts.save(post).getId();
    }

    /** Trim; vacía → null; truncada a 500 (el límite de la columna, V55). */
    private static String normalizeCaption(String raw) {
        if (raw == null) return null;
        String c = raw.trim();
        if (c.isEmpty()) return null;
        return c.length() > 500 ? c.substring(0, 500) : c;
    }

    /**
     * Construye un post con los snapshots (nombres, grado, modalidad, roca).
     * Modalidad: la del cliente si es válida; si no, la de la propia piedra.
     */
    private FeedPostJpaEntity newPost(String uid, SchoolBlockJpaEntity block,
                                      BlockLineJpaEntity line, String kind, String discipline) {
        var school = block.getSchoolId() == null ? null
                : schools.findById(block.getSchoolId()).orElse(null);

        FeedPostJpaEntity post = new FeedPostJpaEntity(
                uid, block.getSchoolId(),
                school != null ? school.getName() : null,
                block.getId(), block.getName(),
                line != null ? line.getId() : null,
                line != null ? line.getName() : null,
                line != null ? line.getGrade() : null,
                kind);
        post.setDiscipline(normalizeDiscipline(discipline, block));
        post.setRockType(school != null ? school.getRockType() : null);
        return post;
    }

    /** BOULDER | ROUTE del cliente, o derivada de la piedra si null/desconocida. */
    private static String normalizeDiscipline(String raw, SchoolBlockJpaEntity block) {
        if (raw != null) {
            String d = raw.trim().toUpperCase();
            if ("BOULDER".equals(d) || "ROUTE".equals(d)) return d;
        }
        return block.getDiscipline() != null ? block.getDiscipline().name() : null;
    }

    /**
     * Post AUTOMÁTICO al aprobar una contribución (NEW_BLOCK / NEW_LINE), con
     * autor = autor de la contribución. Sin push ni notificación (decisión de
     * diseño: la aprobación ya avisa por email). El que llama debe envolverlo
     * en try/catch: crear el post nunca puede tumbar la aprobación.
     */
    @Transactional
    public long publishSystem(String authorUid, SchoolBlockJpaEntity block,
                              BlockLineJpaEntity line, String kind) {
        if (!KIND_NEW_BLOCK.equals(kind) && !KIND_NEW_LINE.equals(kind)) {
            throw new IllegalArgumentException("kind debe ser NEW_BLOCK o NEW_LINE");
        }
        return posts.save(newPost(authorUid, block, line, kind, null)).getId();
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
        FeedPostJpaEntity post = posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post no encontrado"));
        if (!likes.existsById(new FeedLikeJpaEntity.Key(postId, uid))) {
            likes.save(new FeedLikeJpaEntity(postId, uid));
            // Solo al CREAR el like (no en repeticiones ni unlike) y nunca a uno mismo.
            if (!post.getUserUid().equals(uid)) {
                notifyLike(uid, post);
            }
        }
        return countLikes(postId);
    }

    /** Notifica al dueño del post que alguien le ha dado like. Nunca tumba la tx. */
    private void notifyLike(String likerUid, FeedPostJpaEntity post) {
        try {
            User liker = users.findByUid(likerUid).orElse(null);
            String name = displayNameOf(liker);
            String body = "A " + name + " le gusta tu ascenso de «" + postLabel(post) + "»";
            notifications.create(post.getUserUid(), "FEED_LIKE",
                    "Nuevo me gusta", body, "feed_post", String.valueOf(post.getId()));
            push.sendDataToUserAsync(post.getUserUid(),
                    pushData(String.valueOf(post.getId()), "Nuevo me gusta", body, avatarUrlOf(liker)));
        } catch (Exception e) {
            log.warn("No se pudo notificar el like del post {}: {}", post.getId(), e.getMessage());
        }
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

        // Comentaristas previos ANTES de guardar el nuevo (para no notificarse a sí mismo).
        List<FeedCommentJpaEntity> previous = comments.findByPostIdOrderByCreatedAtAsc(postId);

        FeedCommentJpaEntity saved = comments.save(new FeedCommentJpaEntity(
                UUID.randomUUID().toString(), postId, uid, author, trimmed));

        posts.findById(postId).ifPresent(post -> notifyComment(uid, author, post, previous));

        return new FeedCommentView(saved.getId(), saved.getPostId(), saved.getUid(),
                saved.getAuthor(), saved.getText(), saved.getCreatedAt(), true);
    }

    /**
     * Notifica el comentario nuevo al dueño del post y a los demás usuarios que
     * ya habían comentado (distinct, sin el autor del comentario nuevo y sin
     * duplicar al dueño). Nunca tumba la transacción.
     */
    private void notifyComment(String commenterUid, String commenterName,
                               FeedPostJpaEntity post, List<FeedCommentJpaEntity> previous) {
        try {
            String postIdStr = String.valueOf(post.getId());
            String avatar = avatarUrlOf(users.findByUid(commenterUid).orElse(null));

            String owner = post.getUserUid();
            if (!owner.equals(commenterUid)) {
                String body = "«" + commenterName + "» ha comentado tu ascenso de «" + postLabel(post) + "»";
                notifications.create(owner, "FEED_COMMENT",
                        "Nuevo comentario", body, "feed_post", postIdStr);
                push.sendDataToUserAsync(owner, pushData(postIdStr, "Nuevo comentario", body, avatar));
            }

            List<String> others = previous.stream()
                    .map(FeedCommentJpaEntity::getUid)
                    .distinct()
                    .filter(u -> !u.equals(commenterUid) && !u.equals(owner))
                    .toList();
            if (!others.isEmpty()) {
                String body = "«" + commenterName + "» ha respondido en un ascenso que comentaste";
                for (String u : others) {
                    notifications.create(u, "FEED_COMMENT",
                            "Nuevo comentario", body, "feed_post", postIdStr);
                }
                Map<String, String> data = pushData(postIdStr, "Nuevo comentario", body, avatar);
                push.sendDataToUsersAsync(others, data);
            }
        } catch (Exception e) {
            log.warn("No se pudo notificar el comentario del post {}: {}", post.getId(), e.getMessage());
        }
    }

    /** Data payload de los push del feed (mismas claves que los sociales). */
    private Map<String, String> pushData(String postId, String title, String body, String avatarUrl) {
        Map<String, String> data = new HashMap<>();
        data.put("targetType", "feed_post");
        data.put("targetId", postId);
        data.put("title", title);
        data.put("body", body);
        if (avatarUrl != null && !avatarUrl.isBlank()) data.put("avatarUrl", avatarUrl);
        return data;
    }

    /** «Nombre de la vía» o, si el post no tiene vía, el de la piedra. */
    private static String postLabel(FeedPostJpaEntity post) {
        if (post.getLineName() != null && !post.getLineName().isBlank()) return post.getLineName();
        if (post.getBlockName() != null && !post.getBlockName().isBlank()) return post.getBlockName();
        return "tu vía";
    }

    private static String displayNameOf(User u) {
        if (u == null) return "Alguien";
        if (u.getUsername() != null) return "@" + u.getUsername();
        return u.getDisplayName() != null ? u.getDisplayName() : "Alguien";
    }

    /** URL (firmada si hace falta) de la foto de perfil del actor, o null. */
    private String avatarUrlOf(User u) {
        if (u == null) return null;
        try {
            return mapper.toPublic(u).photoUrl();
        } catch (Exception e) {
            return null;
        }
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
