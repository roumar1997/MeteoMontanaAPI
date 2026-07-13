package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.feed.FeedService;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Feed social (pestaña Comunidad). TODO con token (sin scope público): así el
 * filtrado de bloqueados y la privacidad de perfiles son triviales.
 * SecurityConfig no necesita cambios (anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    /**
     * discipline es opcional (BOULDER | ROUTE); si falta, se deriva de la piedra.
     * caption es opcional: descripción del autor (trim, vacía → null, máx 500).
     */
    public record PublishRequest(String blockId, String lineId, String kind,
                                 String discipline, String caption) {}
    /** parentId opcional (V57): comentario al que se responde (puede ser una respuesta). */
    public record CommentRequest(String text, String parentId) {}

    private final FeedService service;
    private final UserRepository users;

    public FeedController(FeedService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    /**
     * Página del feed. scope=following|all|mine|user; before = id del último
     * post visto. Con scope=user, uid = autor cuyos posts se piden (sección
     * "actividad" del perfil público; privado sin follow aceptado → lista vacía).
     */
    @GetMapping
    public List<FeedService.FeedPostView> page(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String uid,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal FirebaseUser user) {
        if ("user".equalsIgnoreCase(scope)) {
            return service.pageOfUser(user.uid(), uid, before, limit);
        }
        return service.page(user.uid(), scope, before, limit);
    }

    /** UN post por id (destino de las notificaciones de like/comentario). */
    @GetMapping("/{postId}")
    public FeedService.FeedPostView single(
            @PathVariable long postId,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.single(user.uid(), postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> publish(
            @RequestBody PublishRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("id", service.publish(user.uid(), req.blockId(), req.lineId(),
                req.kind(), req.discipline(), req.caption()));
    }

    /**
     * Sube (o reemplaza) la foto de celebración de un post PROPIO (multipart,
     * campo "file", máx 5MB, magic bytes validados). 403 si no eres el dueño.
     */
    @PostMapping("/{postId}/photo")
    public Map<String, String> uploadPhoto(
            @PathVariable long postId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal FirebaseUser user) throws IOException {
        return Map.of("photoUrl", service.uploadPhoto(user.uid(), postId, file));
    }

    @DeleteMapping("/{postId}")
    public void delete(
            @PathVariable long postId,
            @AuthenticationPrincipal FirebaseUser user) {
        service.delete(user.uid(), postId, isAdmin(user));
    }

    @PostMapping("/{postId}/like")
    public Map<String, Long> like(
            @PathVariable long postId,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("likeCount", service.like(user.uid(), postId));
    }

    @DeleteMapping("/{postId}/like")
    public Map<String, Long> unlike(
            @PathVariable long postId,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("likeCount", service.unlike(user.uid(), postId));
    }

    @GetMapping("/{postId}/comments")
    public List<FeedService.FeedCommentView> comments(
            @PathVariable long postId,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.listComments(user.uid(), postId);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedService.FeedCommentView addComment(
            @PathVariable long postId,
            @RequestBody CommentRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.addComment(user.uid(), postId, req.text(), req.parentId());
    }

    @PostMapping("/comments/{commentId}/like")
    public Map<String, Long> likeComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("likeCount", service.likeComment(user.uid(), commentId));
    }

    @DeleteMapping("/comments/{commentId}/like")
    public Map<String, Long> unlikeComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("likeCount", service.unlikeComment(user.uid(), commentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal FirebaseUser user) {
        service.deleteComment(user.uid(), commentId, isAdmin(user));
    }

    private boolean isAdmin(FirebaseUser user) {
        return users.findByUid(user.uid()).map(u -> u.isAdmin()).orElse(false);
    }
}
