package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.FeedComment;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FeedCommentLikeRepository;
import com.meteomontana.api.domain.port.FeedCommentRepository;
import com.meteomontana.api.domain.port.FeedLikeRepository;
import com.meteomontana.api.domain.port.FeedPostRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserBlockRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Red de seguridad de las zonas del feed SIN tests hasta el troceo de
 * FeedService: unlike, listComments, likes de comentario, deleteComment y las
 * MENCIONES @username. Reescrita sobre los PUERTOS de dominio (P2.3b).
 */
class FeedServiceCommentsTest {

    FeedPostRepository posts = mock(FeedPostRepository.class);
    FeedLikeRepository likes = mock(FeedLikeRepository.class);
    FeedCommentRepository comments = mock(FeedCommentRepository.class);
    FeedCommentLikeRepository commentLikes = mock(FeedCommentLikeRepository.class);
    FollowRepository follows = mock(FollowRepository.class);
    UserBlockRepository blocks = mock(UserBlockRepository.class);
    SchoolBlockRepository schoolBlocks = mock(SchoolBlockRepository.class);
    SchoolRepository schools = mock(SchoolRepository.class);
    UserRepository users = mock(UserRepository.class);
    UserDtoMapper mapper = mock(UserDtoMapper.class);
    UserModerationService moderation = mock(UserModerationService.class);
    NotificationService notifications = mock(NotificationService.class);
    PushSender push = mock(PushSender.class);
    com.meteomontana.api.infrastructure.storage.StorageService storage =
            mock(com.meteomontana.api.infrastructure.storage.StorageService.class);

    FeedAccessGuard guard;
    FeedNotifier notifier;
    FeedViewMapper viewMapper;
    FeedPhotoService photoService;
    FeedPublishService publisher;
    FeedLikeService likeService;
    FeedCommentService commentService;
    FeedQueryService queryService;

    @BeforeEach
    void setUp() {
        guard = new FeedAccessGuard(blocks, follows, users);
        notifier = new FeedNotifier(users, mapper, notifications, push);
        viewMapper = new FeedViewMapper(likes, comments, schoolBlocks, users, mapper, storage);
        photoService = new FeedPhotoService(posts, storage);
        publisher = new FeedPublishService(posts, schoolBlocks, schools, moderation, notifier, photoService);
        likeService = new FeedLikeService(posts, likes, notifier);
        commentService = new FeedCommentService(posts, comments, commentLikes, moderation, guard, viewMapper, notifier);
        queryService = new FeedQueryService(posts, follows, guard, viewMapper);
    }

    private User user(String uid, String username) {
        User u = mock(User.class);
        when(u.getUid()).thenReturn(uid);
        when(u.getUsername()).thenReturn(username);
        when(u.isPublic()).thenReturn(true);
        when(mapper.toPublic(u)).thenReturn(
                new PublicProfileDto(uid, username, "Name", null, null, null, false, true));
        return u;
    }

    private FeedComment comment(String id, long postId, String uid) {
        return new FeedComment(id, postId, uid, "@" + uid, "texto", null, null);
    }

    // ------------------------------------------------------------ unlike

    @Test
    void unlikeQuitaElLikeYDevuelveContador() {
        when(likes.countByPostIds(List.of(5L))).thenReturn(Map.of(5L, 3L));

        long count = likeService.unlike("me", 5L);

        verify(likes).remove(5L, "me");   // idempotente: el puerto lo garantiza
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void unlikeSinLikePrevioDevuelveCero() {
        when(likes.countByPostIds(List.of(5L))).thenReturn(Map.of());

        long count = likeService.unlike("me", 5L);

        assertThat(count).isZero();
    }

    // ------------------------------------------------------------ listComments

    @Test
    void listCommentsFiltraAutoresBloqueadosYExponeLikes() {
        when(blocks.blockedUidsOf("me")).thenReturn(Set.of("troll"));
        var c1 = comment("c1", 7L, "amigo");
        var c2 = comment("c2", 7L, "troll");
        when(comments.findByPostId(7L)).thenReturn(List.of(c1, c2));
        User amigo = user("amigo", "amigo");
        when(users.findByUids(any())).thenReturn(List.of(amigo));
        when(commentLikes.countByCommentIds(List.of("c1", "c2"))).thenReturn(Map.of("c1", 4L));
        when(commentLikes.likedCommentIds("me", List.of("c1", "c2"))).thenReturn(Set.of("c1"));

        var out = commentService.listComments("me", 7L);

        assertThat(out).hasSize(1);                       // el del troll fuera
        assertThat(out.get(0).id()).isEqualTo("c1");
        assertThat(out.get(0).likeCount()).isEqualTo(4L);
        assertThat(out.get(0).likedByMe()).isTrue();
    }

    // ------------------------------------------------------------ likeComment

    @Test
    void likeCommentCreaYNotificaAlAutor() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "autor")));
        when(commentLikes.exists("c1", "liker")).thenReturn(false);
        when(commentLikes.countByCommentId("c1")).thenReturn(1L);
        User liker = user("liker", "liker");
        when(users.findByUid("liker")).thenReturn(Optional.of(liker));

        long count = commentService.likeComment("liker", "c1");

        assertThat(count).isEqualTo(1L);
        verify(commentLikes).add("c1", "liker");
        verify(notifications).create(eq("autor"), eq("FEED_COMMENT_LIKE"), any(), any(), eq("feed_post"), eq("7"));
    }

    @Test
    void likeCommentPropioNoSeNotificaASiMismo() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "yo")));
        when(commentLikes.exists("c1", "yo")).thenReturn(false);
        when(commentLikes.countByCommentId("c1")).thenReturn(1L);

        commentService.likeComment("yo", "c1");

        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void likeCommentRepetidoEsIdempotenteYNoRenotifica() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "autor")));
        when(commentLikes.exists("c1", "liker")).thenReturn(true);
        when(commentLikes.countByCommentId("c1")).thenReturn(2L);

        long count = commentService.likeComment("liker", "c1");

        assertThat(count).isEqualTo(2L);
        verify(commentLikes, never()).add(any(), any());
        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unlikeCommentEsIdempotente() {
        when(commentLikes.countByCommentId("c1")).thenReturn(0L);

        assertThat(commentService.unlikeComment("me", "c1")).isZero();
        verify(commentLikes).remove("c1", "me");   // idempotente en el puerto
    }

    // ------------------------------------------------------------ deleteComment

    @Test
    void deleteCommentAjenoSinAdminDa403() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "otro")));
        assertThatThrownBy(() -> commentService.deleteComment("yo", "c1", false))
                .isInstanceOf(ForbiddenException.class);
        verify(comments, never()).deleteById(any());
    }

    @Test
    void deleteCommentPropioOAdminBorra() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "yo")));
        commentService.deleteComment("yo", "c1", false);
        verify(comments).deleteById("c1");

        when(comments.findById("c2")).thenReturn(Optional.of(comment("c2", 7L, "otro")));
        commentService.deleteComment("admin", "c2", true);
        verify(comments).deleteById("c2");
    }

    // ------------------------------------------------------------ menciones

    private FeedPost post(long id, String authorUid) {
        return new FeedPost(id, authorUid, null, null, "b1", null, null, null, null,
                FeedViews.KIND_TICK, null);
    }

    @Test
    void addCommentNotificaALosMencionados() {
        when(posts.existsById(7L)).thenReturn(true);
        when(posts.findById(7L)).thenReturn(Optional.of(post(7L, "yo")));
        when(comments.findByPostId(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.create(any())).thenAnswer(inv -> inv.getArgument(0));
        User ana = user("uid-ana", "ana_escaladora");
        when(users.findByUsername("ana_escaladora")).thenReturn(Optional.of(ana));

        commentService.addComment("yo", 7L, "brutal @ana_escaladora, repetimos", null);

        verify(notifications).create(eq("uid-ana"), eq("FEED_MENTION"),
                eq("Te han mencionado"), contains("mencionado"), eq("feed_post"), eq("7"));
    }

    @Test
    void mencionRepetidaNotificaUnaSolaVezYNuncaAUnoMismo() {
        when(posts.existsById(7L)).thenReturn(true);
        when(posts.findById(7L)).thenReturn(Optional.of(post(7L, "yo")));
        when(comments.findByPostId(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.create(any())).thenAnswer(inv -> inv.getArgument(0));
        User ana = user("uid-ana", "ana_escaladora");
        when(users.findByUsername("ana_escaladora")).thenReturn(Optional.of(ana));
        // "yo" se menciona a sí mismo → jamás notificado.
        when(users.findByUsername("yo")).thenReturn(Optional.of(yo));

        commentService.addComment("yo", 7L, "@ana_escaladora y otra vez @ana_escaladora y @yo", null);

        verify(notifications).create(eq("uid-ana"), eq("FEED_MENTION"), any(), any(), any(), any());
        verify(notifications, never()).create(eq("yo"), eq("FEED_MENTION"), any(), any(), any(), any());
    }

    @Test
    void mencionAUsernameInexistenteNoRevienta() {
        when(posts.existsById(7L)).thenReturn(true);
        when(posts.findById(7L)).thenReturn(Optional.of(post(7L, "yo")));
        when(comments.findByPostId(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.create(any())).thenAnswer(inv -> inv.getArgument(0));
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());

        var out = commentService.addComment("yo", 7L, "hola @nadie_conocido", null);

        assertThat(out.text()).contains("@nadie_conocido");  // el comentario se guarda igual
        verify(notifications, never()).create(any(), eq("FEED_MENTION"), any(), any(), any(), any());
    }
}
