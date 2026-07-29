package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.FeedPostRepository;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * ESCRITURA de posts del feed: publicar un ascenso propio (TICK/PROJECT_DONE),
 * el post automático al aprobar contribuciones (NEW_BLOCK/NEW_LINE) y el
 * borrado. Los snapshots (nombres, grado, modalidad, roca) se congelan aquí.
 */
@Service
@RequiredArgsConstructor
public class FeedPublishService {

    private final FeedPostRepository posts;
    private final SchoolBlockRepository schoolBlocks;
    private final SchoolRepository schools;
    private final UserModerationService moderation;
    private final FeedNotifier notifier;
    private final FeedPhotoService photos;

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
            throw new BadRequestException(
                    "kind debe ser TICK o PROJECT_DONE");
        }
        SchoolBlock block = schoolBlocks.findById(blockId)
                .orElseThrow(() -> new NotFoundException("piedra no encontrada"));

        BlockLine line = null;
        if (lineId != null && !lineId.isBlank()) {
            line = block.getLines().stream()
                    .filter(l -> lineId.equals(l.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException(
                            "la vía no pertenece a esa piedra"));
            var existing = posts.findByUserLineAndKind(uid, lineId, kind);
            if (existing.isPresent()) return existing.get().getId();
        }

        FeedPost post = newPost(uid, block, line, kind, discipline);
        String cap = normalizeCaption(caption);
        post.setCaption(cap);
        long id = posts.create(post).getId();
        // Menciones @username en la descripción → notificar a los mencionados.
        notifier.notifyMentions(uid, notifier.authorLabelOf(uid), cap, id);
        return id;
    }

    /**
     * Post AUTOMÁTICO al aprobar una contribución (NEW_BLOCK / NEW_LINE), con
     * autor = autor de la contribución. Sin push ni notificación (la aprobación
     * ya avisa por email). El que llama debe envolverlo en try/catch: crear el
     * post nunca puede tumbar la aprobación. Recibe IDS (no entidades): la
     * piedra se relee por su puerto — frontera limpia con la capa de
     * contribuciones.
     */
    @Transactional
    public long publishSystem(String authorUid, String blockId, String lineId, String kind) {
        if (!FeedViews.KIND_NEW_BLOCK.equals(kind) && !FeedViews.KIND_NEW_LINE.equals(kind)) {
            throw new IllegalArgumentException("kind debe ser NEW_BLOCK o NEW_LINE");
        }
        SchoolBlock block = schoolBlocks.findById(blockId)
                .orElseThrow(() -> new NotFoundException("piedra no encontrada"));
        BlockLine line = (lineId == null || lineId.isBlank()) ? null
                : block.getLines().stream()
                        .filter(l -> lineId.equals(l.getId()))
                        .findFirst().orElse(null);
        return posts.create(newPost(authorUid, block, line, kind, null)).getId();
    }

    /** Borra un post propio (o cualquiera si es admin). */
    @Transactional
    public void delete(String uid, long postId, boolean isAdmin) {
        FeedPost p = posts.findById(postId)
                .orElseThrow(() -> new NotFoundException("post no encontrado"));
        if (!isAdmin && !p.getUserUid().equals(uid)) {
            throw new ForbiddenException("solo puedes borrar tus posts");
        }
        posts.deleteById(postId); // likes y comentarios caen por ON DELETE CASCADE
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
    private FeedPost newPost(String uid, SchoolBlock block,
                             BlockLine line, String kind, String discipline) {
        var school = block.getSchoolId() == null ? null
                : schools.findById(block.getSchoolId()).orElse(null);

        FeedPost post = new FeedPost(
                null, uid, block.getSchoolId(),
                school != null ? school.getName() : null,
                block.getId(), block.getName(),
                line != null ? line.getId() : null,
                line != null ? line.getName() : null,
                line != null ? line.getGrade() : null,
                kind, null);
        post.setDiscipline(normalizeDiscipline(discipline, block));
        post.setRockType(school != null ? school.getRockType() : null);
        return post;
    }

    /** BOULDER | ROUTE del cliente, o derivada de la piedra si null/desconocida. */
    private static String normalizeDiscipline(String raw, SchoolBlock block) {
        if (raw != null) {
            String d = raw.trim().toUpperCase();
            if ("BOULDER".equals(d) || "ROUTE".equals(d)) return d;
        }
        return block.getDiscipline() != null ? block.getDiscipline().name() : null;
    }
}
