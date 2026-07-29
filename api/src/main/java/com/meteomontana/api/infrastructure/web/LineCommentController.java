package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.comments.LineCommentService;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Comentarios + votos de utilidad en piedras/muros y vías. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LineCommentController {

    public record CreateCommentRequest(String lineId, String text) {}
    public record VoteRequest(int value) {}

    private final LineCommentService service;
    private final UserRepository users;

    /** Lectura pública; con token, cada comentario trae myVote. */
    @GetMapping("/blocks/{blockId}/comments")
    public List<LineCommentService.CommentView> list(
            @PathVariable String blockId,
            @RequestParam(required = false) String lineId,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.list(blockId, lineId, user != null ? user.uid() : null);
    }

    @PostMapping("/blocks/{blockId}/comments")
    public LineCommentService.CommentView create(
            @PathVariable String blockId,
            @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return service.add(user.uid(), blockId, req.lineId(), req.text());
    }

    @PostMapping("/line-comments/{commentId}/vote")
    public Map<String, Integer> vote(
            @PathVariable String commentId,
            @RequestBody VoteRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return Map.of("myVote", service.vote(user.uid(), commentId, req.value()));
    }

    @DeleteMapping("/line-comments/{commentId}")
    public void delete(
            @PathVariable String commentId,
            @AuthenticationPrincipal FirebaseUser user) {
        boolean isAdmin = users.findByUid(user.uid()).map(u -> u.isAdmin()).orElse(false);
        service.delete(user.uid(), commentId, isAdmin);
    }
}
