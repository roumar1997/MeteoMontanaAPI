package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFollowRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
 * Red de seguridad de las zonas del feed SIN tests hasta hoy (paso 0 del
 * refactor de FeedService): unlike, listComments, likes de comentario,
 * deleteComment y las MENCIONES @username. Se escriben sobre el FeedService
 * ACTUAL para que el troceo posterior tenga red completa.
 */
class FeedServiceCommentsTest {

    SpringDataFeedPostRepository posts = mock(SpringDataFeedPostRepository.class);
    SpringDataFeedLikeRepository likes = mock(SpringDataFeedLikeRepository.class);
    SpringDataFeedCommentRepository comments = mock(SpringDataFeedCommentRepository.class);
    SpringDataFeedCommentLikeRepository commentLikes = mock(SpringDataFeedCommentLikeRepository.class);
    SpringDataFollowRepository follows = mock(SpringDataFollowRepository.class);
    SpringDataUserBlockRepository blocks = mock(SpringDataUserBlockRepository.class);
    SpringDataSchoolBlockRepository schoolBlocks = mock(SpringDataSchoolBlockRepository.class);
    SpringDataSchoolRepository schools = mock(SpringDataSchoolRepository.class);
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

    private FeedCommentJpaEntity comment(String id, long postId, String uid) {
        return new FeedCommentJpaEntity(id, postId, uid, "@" + uid, "texto", null);
    }

    // ------------------------------------------------------------ unlike

    @Test
    void unlikeBorraElLikeExistenteYDevuelveContador() {
        var key = new FeedLikeJpaEntity.Key(5L, "me");
        when(likes.existsById(key)).thenReturn(true);
        when(likes.countByPostIds(List.of(5L))).thenReturn(java.util.Collections.singletonList(new Object[]{5L, 3L}));

        long count = likeService.unlike("me", 5L);

        verify(likes).deleteById(key);
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void unlikeEsIdempotenteSiNoHabiaLike() {
        when(likes.existsById(any(FeedLikeJpaEntity.Key.class))).thenReturn(false);
        when(likes.countByPostIds(List.of(5L))).thenReturn(List.of());

        long count = likeService.unlike("me", 5L);

        verify(likes, never()).deleteById(any(FeedLikeJpaEntity.Key.class));
        assertThat(count).isZero();
    }

    // ------------------------------------------------------------ listComments

    @Test
    void listCommentsFiltraAutoresBloqueadosYExponeLikes() {
        when(blocks.findByBlockerUid("me")).thenReturn(List.of(new UserBlockJpaEntity("me", "troll")));
        var c1 = comment("c1", 7L, "amigo");
        var c2 = comment("c2", 7L, "troll");
        when(comments.findByPostIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(c1, c2));
        User amigo = user("amigo", "amigo");
        when(users.findByUids(any())).thenReturn(List.of(amigo));
        when(commentLikes.countByCommentIds(List.of("c1", "c2")))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"c1", 4L}));
        when(commentLikes.likedCommentIds("me", List.of("c1", "c2"))).thenReturn(List.of("c1"));

        var out = commentService.listComments("me", 7L);

        assertThat(out).hasSize(1);                       // el del troll fuera
        assertThat(out.get(0).id()).isEqualTo("c1");
        assertThat(out.get(0).likeCount()).isEqualTo(4L);
        assertThat(out.get(0).likedByMe()).isTrue();
    }

    // ------------------------------------------------------------ likeComment

    @Test
    void likeCommentCreaYNotificaAlAutor() {
        var c = comment("c1", 7L, "autor");
        when(comments.findById("c1")).thenReturn(Optional.of(c));
        when(commentLikes.existsById(any(FeedCommentLikeJpaEntity.Key.class))).thenReturn(false);
        when(commentLikes.countByCommentId("c1")).thenReturn(1L);
        User liker = user("liker", "liker");
        when(users.findByUid("liker")).thenReturn(Optional.of(liker));

        long count = commentService.likeComment("liker", "c1");

        assertThat(count).isEqualTo(1L);
        verify(commentLikes).save(any(FeedCommentLikeJpaEntity.class));
        verify(notifications).create(eq("autor"), eq("FEED_COMMENT_LIKE"), any(), any(), eq("feed_post"), eq("7"));
    }

    @Test
    void likeCommentPropioNoSeNotificaASiMismo() {
        var c = comment("c1", 7L, "yo");
        when(comments.findById("c1")).thenReturn(Optional.of(c));
        when(commentLikes.existsById(any(FeedCommentLikeJpaEntity.Key.class))).thenReturn(false);
        when(commentLikes.countByCommentId("c1")).thenReturn(1L);

        commentService.likeComment("yo", "c1");

        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void likeCommentRepetidoEsIdempotenteYNoRenotifica() {
        var c = comment("c1", 7L, "autor");
        when(comments.findById("c1")).thenReturn(Optional.of(c));
        when(commentLikes.existsById(any(FeedCommentLikeJpaEntity.Key.class))).thenReturn(true);
        when(commentLikes.countByCommentId("c1")).thenReturn(2L);

        long count = commentService.likeComment("liker", "c1");

        assertThat(count).isEqualTo(2L);
        verify(commentLikes, never()).save(any(FeedCommentLikeJpaEntity.class));
        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unlikeCommentEsIdempotente() {
        when(commentLikes.existsById(any(FeedCommentLikeJpaEntity.Key.class))).thenReturn(false);
        when(commentLikes.countByCommentId("c1")).thenReturn(0L);

        assertThat(commentService.unlikeComment("me", "c1")).isZero();
        verify(commentLikes, never()).deleteById(any(FeedCommentLikeJpaEntity.Key.class));
    }

    // ------------------------------------------------------------ deleteComment

    @Test
    void deleteCommentAjenoSinAdminDa403() {
        when(comments.findById("c1")).thenReturn(Optional.of(comment("c1", 7L, "otro")));
        assertThatThrownBy(() -> commentService.deleteComment("yo", "c1", false))
                .isInstanceOf(ForbiddenException.class);
        verify(comments, never()).delete(any());
    }

    @Test
    void deleteCommentPropioOAdminBorra() {
        var propio = comment("c1", 7L, "yo");
        when(comments.findById("c1")).thenReturn(Optional.of(propio));
        commentService.deleteComment("yo", "c1", false);
        verify(comments).delete(propio);

        var ajeno = comment("c2", 7L, "otro");
        when(comments.findById("c2")).thenReturn(Optional.of(ajeno));
        commentService.deleteComment("admin", "c2", true);
        verify(comments).delete(ajeno);
    }

    // ------------------------------------------------------------ menciones

    private FeedPostJpaEntity post(long id, String authorUid) {
        FeedPostJpaEntity p = mock(FeedPostJpaEntity.class);
        when(p.getId()).thenReturn(id);
        when(p.getUserUid()).thenReturn(authorUid);
        return p;
    }

    @Test
    void addCommentNotificaALosMencionados() {
        when(posts.existsById(7L)).thenReturn(true);
        FeedPostJpaEntity p7 = post(7L, "yo");
        when(posts.findById(7L)).thenReturn(Optional.of(p7));
        when(comments.findByPostIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User ana = user("uid-ana", "ana_escaladora");
        when(users.findByUsername("ana_escaladora")).thenReturn(Optional.of(ana));

        commentService.addComment("yo", 7L, "brutal @ana_escaladora, repetimos", null);

        verify(notifications).create(eq("uid-ana"), eq("FEED_MENTION"),
                eq("Te han mencionado"), contains("mencionado"), eq("feed_post"), eq("7"));
    }

    @Test
    void mencionRepetidaNotificaUnaSolaVezYNuncaAUnoMismo() {
        when(posts.existsById(7L)).thenReturn(true);
        FeedPostJpaEntity p7 = post(7L, "yo");
        when(posts.findById(7L)).thenReturn(Optional.of(p7));
        when(comments.findByPostIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));
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
        FeedPostJpaEntity p7 = post(7L, "yo");
        when(posts.findById(7L)).thenReturn(Optional.of(p7));
        when(comments.findByPostIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());
        User yo = user("yo", "yo");
        when(users.findByUid("yo")).thenReturn(Optional.of(yo));
        when(users.findByUids(any())).thenReturn(List.of(yo));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());

        var out = commentService.addComment("yo", 7L, "hola @nadie_conocido", null);

        assertThat(out.text()).contains("@nadie_conocido");  // el comentario se guarda igual
        verify(notifications, never()).create(any(), eq("FEED_MENTION"), any(), any(), any(), any());
    }
}
