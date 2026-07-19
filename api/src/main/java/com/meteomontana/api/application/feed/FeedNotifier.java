package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NOTIFICACIONES del feed (campanita + push): likes, comentarios, likes de
 * comentario y menciones @username. Todo best-effort: un fallo aquí JAMÁS
 * tumba la transacción que lo dispara (try/catch por método) y el push es
 * async para no retener la conexión.
 */
@Service
public class FeedNotifier {

    private static final Logger log = LoggerFactory.getLogger(FeedNotifier.class);

    /** Regex de mención: @ + username (3-20, minúsculas/dígitos/_), sin cortar
     *  a mitad de palabra. Sobre el texto en minúsculas (los username lo son). */
    private static final java.util.regex.Pattern MENTION_PATTERN =
            java.util.regex.Pattern.compile("@([a-z0-9_]{3,20})(?![a-z0-9_])");

    private final UserRepository users;
    private final UserDtoMapper mapper;
    private final NotificationService notifications;
    private final PushSender push;

    public FeedNotifier(UserRepository users, UserDtoMapper mapper,
                        NotificationService notifications, PushSender push) {
        this.users = users;
        this.mapper = mapper;
        this.notifications = notifications;
        this.push = push;
    }

    /** Notifica al dueño del post que alguien le ha dado like. Nunca tumba la tx. */
    public void notifyLike(String likerUid, FeedPostJpaEntity post) {
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

    /**
     * Notifica el comentario nuevo al dueño del post y a los demás usuarios que
     * ya habían comentado (distinct, sin el autor del comentario nuevo y sin
     * duplicar al dueño). Nunca tumba la transacción.
     */
    public void notifyComment(String commenterUid, String commenterName,
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

    /** Notifica al autor del comentario que alguien le ha dado like. Nunca tumba la tx. */
    public void notifyCommentLike(String likerUid, FeedCommentJpaEntity c) {
        try {
            User liker = users.findByUid(likerUid).orElse(null);
            String name = displayNameOf(liker);
            String body = "A " + name + " le gusta tu comentario";
            String postIdStr = String.valueOf(c.getPostId());
            notifications.create(c.getUid(), "FEED_COMMENT_LIKE",
                    "Nuevo me gusta", body, "feed_post", postIdStr);
            push.sendDataToUserAsync(c.getUid(),
                    pushData(postIdStr, "Nuevo me gusta", body, avatarUrlOf(liker)));
        } catch (Exception e) {
            log.warn("No se pudo notificar el like del comentario {}: {}", c.getId(), e.getMessage());
        }
    }

    /**
     * Notifica a los usuarios mencionados con @username en un texto (comentario
     * o descripción de post). Una vez por username, nunca a uno mismo. Nunca
     * tumba la transacción.
     */
    public void notifyMentions(String authorUid, String authorName, String text, long postId) {
        if (text == null || text.isEmpty()) return;
        try {
            java.util.Set<String> done = new java.util.HashSet<>();
            var m = MENTION_PATTERN.matcher(text.toLowerCase());
            String postIdStr = String.valueOf(postId);
            String avatar = avatarUrlOf(users.findByUid(authorUid).orElse(null));
            while (m.find()) {
                String username = m.group(1);
                if (!done.add(username)) continue;
                User u = users.findByUsername(username).orElse(null);
                if (u == null || u.getUid().equals(authorUid)) continue;
                String body = "«" + authorName + "» te ha mencionado";
                notifications.create(u.getUid(), "FEED_MENTION",
                        "Te han mencionado", body, "feed_post", postIdStr);
                push.sendDataToUserAsync(u.getUid(), pushData(postIdStr, "Te han mencionado", body, avatar));
            }
        } catch (Exception e) {
            log.warn("No se pudieron notificar menciones del post {}: {}", postId, e.getMessage());
        }
    }

    /** "@username" / displayName / "Anónimo" para firmar comentarios y avisos. */
    public String authorLabelOf(String uid) {
        return users.findByUid(uid)
                .map(u -> u.getUsername() != null ? "@" + u.getUsername()
                        : (u.getDisplayName() != null ? u.getDisplayName() : "Anónimo"))
                .orElse("Anónimo");
    }

    // ------------------------------------------------------------ helpers

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
}
