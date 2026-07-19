package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ESCRITURA de posts del feed: publicar un ascenso propio (TICK/PROJECT_DONE),
 * el post automático al aprobar contribuciones (NEW_BLOCK/NEW_LINE) y el
 * borrado. Los snapshots (nombres, grado, modalidad, roca) se congelan aquí.
 */
@Service
public class FeedPublishService {

    private final SpringDataFeedPostRepository posts;
    private final SpringDataSchoolBlockRepository schoolBlocks;
    private final SpringDataSchoolRepository schools;
    private final UserModerationService moderation;
    private final FeedNotifier notifier;
    private final FeedPhotoService photos;

    public FeedPublishService(SpringDataFeedPostRepository posts,
                              SpringDataSchoolBlockRepository schoolBlocks,
                              SpringDataSchoolRepository schools,
                              UserModerationService moderation,
                              FeedNotifier notifier,
                              FeedPhotoService photos) {
        this.posts = posts;
        this.schoolBlocks = schoolBlocks;
        this.schools = schools;
        this.moderation = moderation;
        this.notifier = notifier;
        this.photos = photos;
    }

    /**
     * Publica un ascenso propio (TICK / PROJECT_DONE). Idempotente: repetir la
     * misma vía+tipo devuelve el post existente en vez de duplicarlo.
     * NEW_BLOCK/NEW_LINE no se aceptan desde el cliente.
     */
    @Transactional
    public long publish(String uid, String blockId, String lineId, String kind) {
        return publish(uid, blockId, lineId, kind, null, null);
    }

    /**
     * @param discipline modalidad opcional enviada por el cliente (BOULDER | ROUTE).
     *        Si viene null/desconocida se deriva de la piedra; se snapshotea en
     *        el post junto al rock_type de la escuela.
     * @param caption descripción opcional del autor: se trimea, vacía → null,
     *        truncada a 500. Si el post ya existía (idempotencia) NO se toca.
     */
    @Transactional
    public long publish(String uid, String blockId, String lineId, String kind,
                        String discipline, String caption) {
        moderation.ensureCanPost(uid);
        if (!FeedViews.KIND_TICK.equals(kind) && !FeedViews.KIND_PROJECT_DONE.equals(kind)) {
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
        String cap = normalizeCaption(caption);
        post.setCaption(cap);
        long id = posts.save(post).getId();
        // Menciones @username en la descripción → notificar a los mencionados.
        notifier.notifyMentions(uid, notifier.authorLabelOf(uid), cap, id);
        return id;
    }

    /**
     * Post AUTOMÁTICO al aprobar una contribución (NEW_BLOCK / NEW_LINE), con
     * autor = autor de la contribución. Sin push ni notificación (la aprobación
     * ya avisa por email). El que llama debe envolverlo en try/catch: crear el
     * post nunca puede tumbar la aprobación.
     */
    @Transactional
    public long publishSystem(String authorUid, SchoolBlockJpaEntity block,
                              BlockLineJpaEntity line, String kind) {
        if (!FeedViews.KIND_NEW_BLOCK.equals(kind) && !FeedViews.KIND_NEW_LINE.equals(kind)) {
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
        photos.deletePhotoQuietly(p.getPhotoPath());
    }

    // ------------------------------------------------------------ helpers

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
}
