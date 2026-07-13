package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedPostJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedCommentRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedLikeRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFeedPostRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataFollowRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedCommentJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FeedLikeJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.FollowJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
import com.meteomontana.api.domain.port.PushSender;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedServiceTest {

    SpringDataFeedPostRepository posts = mock(SpringDataFeedPostRepository.class);
    SpringDataFeedLikeRepository likes = mock(SpringDataFeedLikeRepository.class);
    SpringDataFeedCommentRepository comments = mock(SpringDataFeedCommentRepository.class);
    SpringDataFollowRepository follows = mock(SpringDataFollowRepository.class);
    SpringDataUserBlockRepository blocks = mock(SpringDataUserBlockRepository.class);
    SpringDataSchoolBlockRepository schoolBlocks = mock(SpringDataSchoolBlockRepository.class);
    SpringDataSchoolRepository schools = mock(SpringDataSchoolRepository.class);
    UserRepository users = mock(UserRepository.class);
    UserDtoMapper mapper = mock(UserDtoMapper.class);
    UserModerationService moderation = mock(UserModerationService.class);
    NotificationService notifications = mock(NotificationService.class);
    PushSender push = mock(PushSender.class);

    FeedService service;

    @BeforeEach
    void setUp() {
        service = new FeedService(posts, likes, comments, follows, blocks,
                schoolBlocks, schools, users, mapper, moderation, notifications, push);
    }

    // ------------------------------------------------------------ helpers

    private User user(String uid, boolean isPublic) {
        User u = mock(User.class);
        when(u.getUid()).thenReturn(uid);
        when(u.isPublic()).thenReturn(isPublic);
        return u;
    }

    private PublicProfileDto profile(String uid, String username) {
        return new PublicProfileDto(uid, username, "Name " + username, null, null, null, false, true);
    }

    private FeedPostJpaEntity post(String authorUid) {
        FeedPostJpaEntity p = mock(FeedPostJpaEntity.class);
        when(p.getId()).thenReturn(1L);
        when(p.getUserUid()).thenReturn(authorUid);
        when(p.getBlockId()).thenReturn("b1");
        when(p.getKind()).thenReturn(FeedService.KIND_TICK);
        return p;
    }

    // ------------------------------------------------------------ page

    @Test
    void followingScopeQueriesAcceptedFollowingPlusSelf() {
        when(follows.findFollowingOf("me")).thenReturn(List.of("friend"));
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());

        service.page("me", "following", null, 20);

        verify(posts).pageByAuthors(eq(List.of("friend", "me")), eq(Long.MAX_VALUE), eq(20));
        verify(posts, never()).pageAllPublic(anyLong(), anyInt());
    }

    @Test
    void allScopeUsesPublicOnlyQuery() {
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of());
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());

        service.page("me", "all", 99L, 10);

        verify(posts).pageAllPublic(99L, 10);
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void blockedAuthorsAreFilteredOut() {
        FeedPostJpaEntity trollPost = post("troll");
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(trollPost));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of(new UserBlockJpaEntity("me", "troll")));

        var result = service.page("me", "all", null, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void deletedAuthorsAreDroppedFromFeed() {
        FeedPostJpaEntity ghostPost = post("ghost");
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(ghostPost));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        when(likes.countByPostIds(anyList())).thenReturn(List.of());
        when(comments.countByPostIds(anyList())).thenReturn(List.of());
        when(likes.likedPostIds(any(), anyList())).thenReturn(List.of());
        when(users.findByUids(anyList())).thenReturn(List.of()); // cuenta borrada
        when(schoolBlocks.findAllById(any())).thenReturn(List.of());

        var result = service.page("me", "all", null, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void pageMapsAuthorAndCounts() {
        FeedPostJpaEntity anaPost = post("ana");
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(anaPost));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        when(likes.countByPostIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(comments.countByPostIds(anyList())).thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        when(likes.likedPostIds(eq("me"), anyList())).thenReturn(List.of(1L));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));
        when(schoolBlocks.findAllById(any())).thenReturn(List.of());

        var result = service.page("me", "all", null, 20);

        assertThat(result).hasSize(1);
        var v = result.get(0);
        assertThat(v.author().username()).isEqualTo("ana");
        assertThat(v.likeCount()).isEqualTo(3);
        assertThat(v.commentCount()).isEqualTo(2);
        assertThat(v.likedByMe()).isTrue();
        assertThat(v.mine()).isFalse();
    }

    @Test
    void mineScopeQueriesOnlySelf() {
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());

        service.page("me", "mine", null, 20);

        verify(posts).pageByAuthors(eq(List.of("me")), eq(Long.MAX_VALUE), eq(20));
        verify(posts, never()).pageAllPublic(anyLong(), anyInt());
    }

    // ------------------------------------------------------------ scope user (perfil público)

    @Test
    void userScopeReturnsEmptyIfTargetPrivateAndCallerNotFollower() {
        User ana = user("ana", false);
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        // follows → Optional.empty() por defecto (no la sigue)

        var result = service.pageOfUser("me", "ana", null, 20);

        assertThat(result).isEmpty();
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void userScopeReturnsEmptyIfCallerBlockedTarget() {
        when(blocks.findByBlockerUid("me")).thenReturn(List.of(new UserBlockJpaEntity("me", "troll")));

        var result = service.pageOfUser("me", "troll", null, 20);

        assertThat(result).isEmpty();
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void userScopeAllowsAcceptedFollowerOfPrivateTarget() {
        User ana = user("ana", false);
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        FollowJpaEntity accepted = mock(FollowJpaEntity.class);
        when(accepted.getStatus()).thenReturn("ACCEPTED");
        when(follows.findById_FollowerUidAndId_FollowedUid("me", "ana"))
                .thenReturn(Optional.of(accepted));
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());

        service.pageOfUser("me", "ana", null, 20);

        verify(posts).pageByAuthors(eq(List.of("ana")), eq(Long.MAX_VALUE), eq(20));
    }

    @Test
    void userScopeOnSelfSkipsPrivacyChecks() {
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());

        service.pageOfUser("me", "me", null, 20);

        verify(posts).pageByAuthors(eq(List.of("me")), eq(Long.MAX_VALUE), eq(20));
        verify(users, never()).findByUid(any());
    }

    // ------------------------------------------------------------ single

    private FeedPostJpaEntity singlePost(long id, String authorUid) {
        FeedPostJpaEntity p = mock(FeedPostJpaEntity.class);
        when(p.getId()).thenReturn(id);
        when(p.getUserUid()).thenReturn(authorUid);
        when(p.getBlockId()).thenReturn("b1");
        return p;
    }

    @Test
    void singleReturns404IfMissing() {
        when(posts.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.single("me", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void singleReturns404IfAuthorPrivateAndCallerNotFollower() {
        FeedPostJpaEntity p = singlePost(5L, "ana");
        User ana = user("ana", false);
        when(posts.findById(5L)).thenReturn(Optional.of(p));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        // follows.findById_... → Optional.empty() por defecto (no la sigue)

        assertThatThrownBy(() -> service.single("me", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void singleReturns404IfCallerBlockedAuthor() {
        FeedPostJpaEntity p = singlePost(5L, "troll");
        when(posts.findById(5L)).thenReturn(Optional.of(p));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of(new UserBlockJpaEntity("me", "troll")));

        assertThatThrownBy(() -> service.single("me", 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void singleAllowsAcceptedFollowerOfPrivateAuthor() {
        FeedPostJpaEntity p = singlePost(5L, "ana");
        User ana = user("ana", false);
        when(posts.findById(5L)).thenReturn(Optional.of(p));
        when(blocks.findByBlockerUid("me")).thenReturn(List.of());
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        FollowJpaEntity accepted = mock(FollowJpaEntity.class);
        when(accepted.getStatus()).thenReturn("ACCEPTED");
        when(follows.findById_FollowerUidAndId_FollowedUid("me", "ana"))
                .thenReturn(Optional.of(accepted));
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublicLocked(ana)).thenReturn(profile("ana", "ana"));
        when(schoolBlocks.findAllById(any())).thenReturn(List.of());

        var v = service.single("me", 5L);

        assertThat(v.id()).isEqualTo(5L);
        assertThat(v.author().username()).isEqualTo("ana");
    }

    // ------------------------------------------------------------ notificaciones

    @Test
    void likeNotifiesOwnerOnlyOnCreation() {
        FeedPostJpaEntity p = singlePost(1L, "ana");
        when(posts.findById(1L)).thenReturn(Optional.of(p));
        when(likes.existsById(any(FeedLikeJpaEntity.Key.class))).thenReturn(false);

        service.like("me", 1L);

        verify(notifications).create(eq("ana"), eq("FEED_LIKE"), any(), any(),
                eq("feed_post"), eq("1"));
        verify(push).sendDataToUserAsync(eq("ana"), any());
    }

    @Test
    void likeDoesNotNotifyOnRepeatOrSelfLike() {
        FeedPostJpaEntity p = singlePost(1L, "ana");
        when(posts.findById(1L)).thenReturn(Optional.of(p));
        when(likes.existsById(any(FeedLikeJpaEntity.Key.class))).thenReturn(true);
        service.like("me", 1L); // repetido → nada

        FeedPostJpaEntity own = singlePost(2L, "me");
        when(posts.findById(2L)).thenReturn(Optional.of(own));
        when(likes.existsById(any(FeedLikeJpaEntity.Key.class))).thenReturn(false);
        service.like("me", 2L); // auto-like → nada

        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    private FeedCommentJpaEntity comment(String uid) {
        FeedCommentJpaEntity c = mock(FeedCommentJpaEntity.class);
        when(c.getUid()).thenReturn(uid);
        return c;
    }

    @Test
    void commentNotifiesOwnerAndPreviousCommentersWithoutDuplicates() {
        FeedPostJpaEntity p = singlePost(2L, "ana");
        when(posts.existsById(2L)).thenReturn(true);
        when(posts.findById(2L)).thenReturn(Optional.of(p));
        // Comentaristas previos: la dueña, bob dos veces y yo mismo.
        List<FeedCommentJpaEntity> previous = List.of(
                comment("ana"), comment("bob"), comment("bob"), comment("me"));
        when(comments.findByPostIdOrderByCreatedAtAsc(2L)).thenReturn(previous);
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addComment("me", 2L, "¡Qué máquina!");

        // Dueña: una sola vez (aunque también comentó antes).
        verify(notifications).create(eq("ana"), eq("FEED_COMMENT"), any(), any(),
                eq("feed_post"), eq("2"));
        verify(push).sendDataToUserAsync(eq("ana"), any());
        // Bob: una sola vez pese a sus dos comentarios; yo nunca.
        verify(notifications).create(eq("bob"), eq("FEED_COMMENT"), any(), any(),
                eq("feed_post"), eq("2"));
        verify(notifications, never()).create(eq("me"), any(), any(), any(), any(), any());
        verify(push).sendDataToUsersAsync(eq(List.of("bob")), any());
    }

    // ------------------------------------------------------------ publish

    @Test
    void publishRejectsReservedKinds() {
        assertThatThrownBy(() -> service.publish("me", "b1", null, FeedService.KIND_NEW_BLOCK))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.publish("me", "b1", null, "INVENTED"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void publishIsIdempotentPerUserLineAndKind() {
        SchoolBlockJpaEntity block = mock(SchoolBlockJpaEntity.class);
        com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity line =
                mock(com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity.class);
        when(line.getId()).thenReturn("l1");
        when(block.getLines()).thenReturn(List.of(line));
        when(schoolBlocks.findById("b1")).thenReturn(Optional.of(block));

        FeedPostJpaEntity existing = mock(FeedPostJpaEntity.class);
        when(existing.getId()).thenReturn(42L);
        when(posts.findByUserUidAndLineIdAndKind("me", "l1", FeedService.KIND_TICK))
                .thenReturn(Optional.of(existing));

        long id = service.publish("me", "b1", "l1", FeedService.KIND_TICK);

        assertThat(id).isEqualTo(42L);
        verify(posts, never()).save(any());
    }

    @Test
    void publishRejectsLineOfAnotherBlock() {
        SchoolBlockJpaEntity block = mock(SchoolBlockJpaEntity.class);
        when(block.getLines()).thenReturn(List.of());
        when(schoolBlocks.findById("b1")).thenReturn(Optional.of(block));

        assertThatThrownBy(() -> service.publish("me", "b1", "other-line", FeedService.KIND_TICK))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ------------------------------------------------------------ snapshot discipline/rock

    /** Piedra ROUTE en una escuela de arenisca, con save que devuelve el arg con id. */
    private SchoolBlockJpaEntity blockWithSchool() {
        SchoolBlockJpaEntity block = mock(SchoolBlockJpaEntity.class);
        when(block.getSchoolId()).thenReturn("s1");
        when(block.getLines()).thenReturn(List.of());
        when(block.getDiscipline())
                .thenReturn(com.meteomontana.api.domain.model.SchoolBlock.Discipline.ROUTE);
        when(schoolBlocks.findById("b1")).thenReturn(Optional.of(block));

        var school = mock(com.meteomontana.api.infrastructure.persistence.jpa.SchoolJpaEntity.class);
        when(school.getName()).thenReturn("Albarracín");
        when(school.getRockType()).thenReturn("Arenisca");
        when(schools.findById("s1")).thenReturn(Optional.of(school));

        when(posts.save(any())).thenAnswer(inv -> {
            FeedPostJpaEntity e = org.mockito.Mockito.spy((FeedPostJpaEntity) inv.getArgument(0));
            org.mockito.Mockito.doReturn(7L).when(e).getId();
            return e;
        });
        return block;
    }

    @Test
    void publishSnapshotsRockTypeAndDerivesDisciplineFromBlock() {
        blockWithSchool();

        service.publish("me", "b1", null, FeedService.KIND_TICK, null);

        var captor = org.mockito.ArgumentCaptor.forClass(FeedPostJpaEntity.class);
        verify(posts).save(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("ROUTE"); // de la piedra
        assertThat(captor.getValue().getRockType()).isEqualTo("Arenisca");
        assertThat(captor.getValue().getSchoolName()).isEqualTo("Albarracín");
    }

    @Test
    void publishPrefersValidClientDisciplineAndIgnoresGarbage() {
        blockWithSchool();

        service.publish("me", "b1", null, FeedService.KIND_TICK, "boulder");
        var captor = org.mockito.ArgumentCaptor.forClass(FeedPostJpaEntity.class);
        verify(posts).save(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("BOULDER"); // normalizada

        org.mockito.Mockito.clearInvocations(posts);
        service.publish("me", "b1", null, FeedService.KIND_TICK, "SPEED");
        verify(posts).save(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("ROUTE"); // desconocida → piedra
    }

    // ------------------------------------------------------------ posts automáticos

    @Test
    void publishSystemCreatesNewBlockPostWithSnapshots() {
        SchoolBlockJpaEntity block = blockWithSchool();

        long id = service.publishSystem("author", block, null, FeedService.KIND_NEW_BLOCK);

        assertThat(id).isEqualTo(7L);
        var captor = org.mockito.ArgumentCaptor.forClass(FeedPostJpaEntity.class);
        verify(posts).save(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(FeedService.KIND_NEW_BLOCK);
        assertThat(captor.getValue().getUserUid()).isEqualTo("author");
        assertThat(captor.getValue().getRockType()).isEqualTo("Arenisca");
    }

    @Test
    void publishSystemRejectsClientKinds() {
        SchoolBlockJpaEntity block = mock(SchoolBlockJpaEntity.class);
        assertThatThrownBy(() -> service.publishSystem("author", block, null, FeedService.KIND_TICK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------ delete

    @Test
    void deleteRejectsForeignPostUnlessAdmin() {
        FeedPostJpaEntity p = post("ana");
        when(posts.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.delete("me", 1L, false))
                .isInstanceOf(ResponseStatusException.class);

        service.delete("me", 1L, true); // admin sí puede
        verify(posts).delete(p);
    }
}
