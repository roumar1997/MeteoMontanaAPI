package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.feed.FeedViews.FeedAuthor;
import com.meteomontana.api.application.feed.FeedViews.FeedLineView;
import com.meteomontana.api.application.feed.FeedViews.FeedPostView;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.FeedComment;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FeedCommentRepository;
import com.meteomontana.api.domain.port.FeedLikeRepository;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.meteomontana.api.infrastructure.storage.StorageService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MAPEO dominio → vista del feed: contadores en batch (sin N+1), autor con la
 * regla de privacidad "locked", y foto/trazo/nombre/grado EN VIVO desde
 * la piedra (si se re-dibuja el topo o se renombra la vía, el feed lo
 * refleja; el snapshot del post queda como respaldo si se borran).
 */
@Service
public class FeedViewMapper {

    private static final Logger log = LoggerFactory.getLogger(FeedViewMapper.class);

    /** URL firmada válida 60 min (mismo TTL que las fotos de escuela). */
    static final int PHOTO_URL_TTL_MINUTES = 60;

    private final FeedLikeRepository likes;
    private final FeedCommentRepository comments;
    private final SchoolBlockRepository schoolBlocks;
    private final UserRepository users;
    private final UserDtoMapper mapper;
    private final StorageService storage;

    public FeedViewMapper(FeedLikeRepository likes,
                          FeedCommentRepository comments,
                          SchoolBlockRepository schoolBlocks,
                          UserRepository users,
                          UserDtoMapper mapper,
                          StorageService storage) {
        this.likes = likes;
        this.comments = comments;
        this.schoolBlocks = schoolBlocks;
        this.users = users;
        this.mapper = mapper;
        this.storage = storage;
    }

    /** Mapea posts ya filtrados a vistas (contadores, autor, foto/trazo en vivo). */
    public List<FeedPostView> mapViews(String uid, List<FeedPost> page) {
        List<Long> ids = page.stream().map(FeedPost::getId).toList();

        Map<Long, Long> likeCounts = likes.countByPostIds(ids);
        Map<Long, Long> commentCounts = comments.countByPostIds(ids);
        Set<Long> mine = likes.likedPostIds(uid, ids);

        Map<String, FeedAuthor> authors = loadAuthors(
                page.stream().map(FeedPost::getUserUid).distinct().toList());

        // Foto y trazo se leen EN VIVO de la piedra (si se re-dibuja el topo,
        // el feed lo refleja). Una query por página, no por post.
        Map<String, SchoolBlock> blocksById = schoolBlocks
                .findByIds(page.stream().map(FeedPost::getBlockId).distinct().toList())
                .stream().collect(Collectors.toMap(SchoolBlock::getId, Function.identity()));

        return page.stream().map(p -> {
            FeedAuthor author = authors.get(p.getUserUid());
            if (author == null) return null; // cuenta borrada → fuera del feed

            String photoPath = null;
            String linePath = null;
            String startType = null;
            List<FeedLineView> blockLines = null;
            String liveBlockName = null;
            String liveLineName = null;
            String liveGrade = null;
            SchoolBlock block = blocksById.get(p.getBlockId());
            if (block != null) {
                photoPath = block.getPhotoPath();
                liveBlockName = block.getName();
                if (p.getLineId() != null) {
                    BlockLine line = block.getLines().stream()
                            .filter(l -> p.getLineId().equals(l.getId()))
                            .findFirst().orElse(null);
                    if (line != null) {
                        linePath = line.getLinePath();
                        if (line.getPhotoPath() != null) photoPath = line.getPhotoPath();
                        if (line.getStartType() != null) startType = line.getStartType().name();
                        liveLineName = line.getName();
                        liveGrade = line.getGrade();
                    }
                } else if (FeedViews.KIND_NEW_BLOCK.equals(p.getKind())) {
                    // Piedra nueva: el post no tiene lineId → mandamos las vías de
                    // la cara PORTADA para que las apps las dibujen sobre la foto.
                    String cover = block.getPhotoPath();
                    blockLines = block.getLines().stream()
                            .filter(l -> l.getLinePath() != null && !l.getLinePath().isBlank())
                            .filter(l -> l.getPhotoPath() == null
                                    || l.getPhotoPath().equals(cover))
                            .map(l -> new FeedLineView(
                                    l.getName(), l.getGrade(),
                                    l.getStartType() != null ? l.getStartType().name() : null,
                                    l.getLinePath()))
                            .toList();
                    if (blockLines.isEmpty()) blockLines = null;
                }
            }
            return new FeedPostView(p.getId(), p.getKind(), p.getCreatedAt(), author,
                    p.getSchoolId(), p.getSchoolName(),
                    p.getBlockId(), liveBlockName != null ? liveBlockName : p.getBlockName(),
                    p.getLineId(), liveLineName != null ? liveLineName : p.getLineName(),
                    liveGrade != null ? liveGrade : p.getGrade(),
                    p.getDiscipline(), p.getRockType(),
                    photoPath, linePath,
                    likeCounts.getOrDefault(p.getId(), 0L),
                    mine.contains(p.getId()),
                    commentCounts.getOrDefault(p.getId(), 0L),
                    p.getUserUid().equals(uid),
                    startType, p.getCaption(), signedPhotoUrl(p.getPhotoPath()),
                    blockLines);
        }).filter(v -> v != null).toList();
    }

    /**
     * URL firmada ({@value #PHOTO_URL_TTL_MINUTES} min) de la foto de
     * celebración, o null si el post no tiene o la firma falla (una foto rota
     * nunca tumba la página del feed).
     */
    public String signedPhotoUrl(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) return null;
        try {
            return storage.signedReadUrl(photoPath, PHOTO_URL_TTL_MINUTES).toString();
        } catch (Exception e) {
            log.warn("No se pudo firmar la foto del feed {}: {}", photoPath, e.getMessage());
            return null;
        }
    }

    /** Autores con la regla del ranking: perfil privado → vista "locked". */
    public Map<String, FeedAuthor> loadAuthors(List<String> uids) {
        Map<String, FeedAuthor> out = new HashMap<>();
        for (User u : users.findByUids(uids)) {
            var profile = u.isPublic() ? mapper.toPublic(u) : mapper.toPublicLocked(u);
            out.put(u.getUid(), new FeedAuthor(u.getUid(), profile.username(),
                    profile.displayName(), profile.photoUrl()));
        }
        return out;
    }

    /**
     * Autor de un comentario para la vista: la ficha real si existe; si la
     * cuenta ya no está, el snapshot guardado como displayName.
     */
    public static FeedAuthor commentAuthor(Map<String, FeedAuthor> authors,
                                           FeedComment c) {
        FeedAuthor a = authors.get(c.uid());
        return a != null ? a : new FeedAuthor(c.uid(), null, c.author(), null);
    }
}
