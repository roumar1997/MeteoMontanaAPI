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

    public record PublishRequest(String blockId, String lineId, String kind) {}
    public record CommentRequest(String text) {}

    private final FeedService service;
    private final UserRepository users;

    public FeedController(FeedService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    /** Página del feed. scope=following|all; before = id del último post visto. */
    @GetMapping
    public List<FeedService.FeedPostView> page(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.page(user.uid(), scope, before, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> publish(
            @RequestBody PublishRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("id", service.publish(user.uid(), req.blockId(), req.lineId(), req.kind()));
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
        return service.addComment(user.uid(), postId, req.text());
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
