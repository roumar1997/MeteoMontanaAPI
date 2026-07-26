package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.FeedComment;
import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.SchoolBlock;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del feed sobre los PUERTOS de dominio (P2.3b): los servicios de
 * application/ ya no ven JPA — los mocks son de FeedPostRepository & cía y
 * los objetos, modelos de dominio reales.
 */
class FeedServiceTest {

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

    /** Post TICK básico (id=1, piedra b1) de un autor dado — modelo de dominio real. */
    private FeedPost post(String authorUid) {
        return new FeedPost(1L, authorUid, null, null, "b1", null, null, null, null,
                FeedViews.KIND_TICK, null);
    }

    /** Copia con id asignado (lo que devuelve create() en el adaptador real). */
    private static FeedPost withId(FeedPost p, long id) {
        FeedPost out = new FeedPost(id, p.getUserUid(), p.getSchoolId(), p.getSchoolName(),
                p.getBlockId(), p.getBlockName(), p.getLineId(), p.getLineName(),
                p.getGrade(), p.getKind(), p.getCreatedAt());
        out.setDiscipline(p.getDiscipline());
        out.setRockType(p.getRockType());
        out.setCaption(p.getCaption());
        out.setPhotoPath(p.getPhotoPath());
        return out;
    }

    private SchoolBlock block(String id, SchoolBlock.Discipline discipline,
                              String photoPath, List<BlockLine> lines) {
        return new SchoolBlock(id, "s1", SchoolBlock.Type.BLOCK, discipline, "Piedra",
                0, 0, photoPath, null, "uid", null, lines, null);
    }

    // ------------------------------------------------------------ page

    @Test
    void followingScopeQueriesAcceptedFollowingPlusSelf() {
        when(follows.followingOf("me")).thenReturn(List.of("friend"));
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());

        queryService.page("me", "following", null, 20);

        verify(posts).pageByAuthors(eq(List.of("friend", "me")), eq(Long.MAX_VALUE), eq(20));
        verify(posts, never()).pageAllPublic(anyLong(), anyInt());
    }

    @Test
    void allScopeUsesPublicOnlyQuery() {
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of());

        queryService.page("me", "all", 99L, 10);

        verify(posts).pageAllPublic(99L, 10);
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void blockedAuthorsAreFilteredOut() {
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(post("troll")));
        when(blocks.blockedUidsOf("me")).thenReturn(Set.of("troll"));

        var result = queryService.page("me", "all", null, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void deletedAuthorsAreDroppedFromFeed() {
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(post("ghost")));
        when(users.findByUids(anyList())).thenReturn(List.of()); // cuenta borrada

        var result = queryService.page("me", "all", null, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void pageMapsAuthorAndCounts() {
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(post("ana")));
        when(likes.countByPostIds(anyList())).thenReturn(Map.of(1L, 3L));
        when(comments.countByPostIds(anyList())).thenReturn(Map.of(1L, 2L));
        when(likes.likedPostIds(eq("me"), anyList())).thenReturn(Set.of(1L));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));

        var result = queryService.page("me", "all", null, 20);

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

        queryService.page("me", "mine", null, 20);

        verify(posts).pageByAuthors(eq(List.of("me")), eq(Long.MAX_VALUE), eq(20));
        verify(posts, never()).pageAllPublic(anyLong(), anyInt());
    }

    // ------------------------------------------------------------ scope user (perfil público)

    @Test
    void userScopeReturnsEmptyIfTargetPrivateAndCallerNotFollower() {
        User ana = user("ana", false);   // fuera del when() — stubbing anidado revienta
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        // follows.isFollowing → false por defecto (no la sigue)

        var result = queryService.pageOfUser("me", "ana", null, 20);

        assertThat(result).isEmpty();
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void userScopeReturnsEmptyIfCallerBlockedTarget() {
        when(blocks.isBlocked("me", "troll")).thenReturn(true);

        var result = queryService.pageOfUser("me", "troll", null, 20);

        assertThat(result).isEmpty();
        verify(posts, never()).pageByAuthors(anyList(), anyLong(), anyInt());
    }

    @Test
    void userScopeAllowsAcceptedFollowerOfPrivateTarget() {
        User ana = user("ana", false);
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        when(follows.isFollowing("me", "ana")).thenReturn(true);
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());

        queryService.pageOfUser("me", "ana", null, 20);

        verify(posts).pageByAuthors(eq(List.of("ana")), eq(Long.MAX_VALUE), eq(20));
    }

    @Test
    void userScopeOnSelfSkipsPrivacyChecks() {
        when(posts.pageByAuthors(anyList(), anyLong(), anyInt())).thenReturn(List.of());

        queryService.pageOfUser("me", "me", null, 20);

        verify(posts).pageByAuthors(eq(List.of("me")), eq(Long.MAX_VALUE), eq(20));
        verify(users, never()).findByUid(any());
    }

    // ------------------------------------------------------------ single

    private FeedPost singlePost(long id, String authorUid) {
        return new FeedPost(id, authorUid, null, null, "b1", null, null, null, null,
                FeedViews.KIND_TICK, null);
    }

    @Test
    void singleReturns404IfMissing() {
        when(posts.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> queryService.single("me", 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("post no encontrado");
    }

    @Test
    void singleReturns404IfAuthorPrivateAndCallerNotFollower() {
        User ana = user("ana", false);
        when(posts.findById(5L)).thenReturn(Optional.of(singlePost(5L, "ana")));
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        // follows.isFollowing → false por defecto (no la sigue)

        assertThatThrownBy(() -> queryService.single("me", 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("post no encontrado");
    }

    @Test
    void singleReturns404IfCallerBlockedAuthor() {
        when(posts.findById(5L)).thenReturn(Optional.of(singlePost(5L, "troll")));
        when(blocks.isBlocked("me", "troll")).thenReturn(true);

        assertThatThrownBy(() -> queryService.single("me", 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("post no encontrado");
    }

    @Test
    void singleAllowsAcceptedFollowerOfPrivateAuthor() {
        User ana = user("ana", false);
        when(posts.findById(5L)).thenReturn(Optional.of(singlePost(5L, "ana")));
        when(users.findByUid("ana")).thenReturn(Optional.of(ana));
        when(follows.isFollowing("me", "ana")).thenReturn(true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublicLocked(ana)).thenReturn(profile("ana", "ana"));

        var v = queryService.single("me", 5L);

        assertThat(v.id()).isEqualTo(5L);
        assertThat(v.author().username()).isEqualTo("ana");
    }

    // ------------------------------------------------------------ notificaciones

    @Test
    void likeNotifiesOwnerOnlyOnCreation() {
        when(posts.findById(1L)).thenReturn(Optional.of(singlePost(1L, "ana")));
        when(likes.exists(1L, "me")).thenReturn(false);

        likeService.like("me", 1L);

        verify(likes).add(1L, "me");
        verify(notifications).create(eq("ana"), eq("FEED_LIKE"), any(), any(),
                eq("feed_post"), eq("1"));
        verify(push).sendDataToUserAsync(eq("ana"), any());
    }

    @Test
    void likeDoesNotNotifyOnRepeatOrSelfLike() {
        when(posts.findById(1L)).thenReturn(Optional.of(singlePost(1L, "ana")));
        when(likes.exists(1L, "me")).thenReturn(true);
        likeService.like("me", 1L); // repetido → nada

        when(posts.findById(2L)).thenReturn(Optional.of(singlePost(2L, "me")));
        when(likes.exists(2L, "me")).thenReturn(false);
        likeService.like("me", 2L); // auto-like → nada

        verify(notifications, never()).create(any(), any(), any(), any(), any(), any());
    }

    private FeedComment comment(String uid) {
        return new FeedComment("c-" + uid + "-" + System.nanoTime(), 2L, uid, "@" + uid, "txt", null, null);
    }

    @Test
    void commentNotifiesOwnerAndPreviousCommentersWithoutDuplicates() {
        when(posts.existsById(2L)).thenReturn(true);
        when(posts.findById(2L)).thenReturn(Optional.of(singlePost(2L, "ana")));
        // Comentaristas previos: la dueña, bob dos veces y yo mismo.
        List<FeedComment> previous = List.of(
                comment("ana"), comment("bob"), comment("bob"), comment("me"));
        when(comments.findByPostId(2L)).thenReturn(previous);
        when(comments.create(any())).thenAnswer(inv -> inv.getArgument(0));

        commentService.addComment("me", 2L, "¡Qué máquina!", null);

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
        assertThatThrownBy(() -> publisher.publish("me", "b1", null, FeedViews.KIND_NEW_BLOCK))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> publisher.publish("me", "b1", null, "INVENTED"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void publishIsIdempotentPerUserLineAndKind() {
        BlockLine line = new BlockLine("l1", "b1", "Vía", "6a", null, null, 0, null, 0);
        when(schoolBlocks.findById("b1"))
                .thenReturn(Optional.of(block("b1", SchoolBlock.Discipline.BOULDER, null, List.of(line))));
        when(posts.findByUserLineAndKind("me", "l1", FeedViews.KIND_TICK))
                .thenReturn(Optional.of(withId(post("me"), 42L)));

        long id = publisher.publish("me", "b1", "l1", FeedViews.KIND_TICK);

        assertThat(id).isEqualTo(42L);
        verify(posts, never()).create(any());
    }

    @Test
    void publishRejectsLineOfAnotherBlock() {
        when(schoolBlocks.findById("b1"))
                .thenReturn(Optional.of(block("b1", SchoolBlock.Discipline.BOULDER, null, List.of())));

        assertThatThrownBy(() -> publisher.publish("me", "b1", "other-line", FeedViews.KIND_TICK))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------ snapshot discipline/rock

    /** Piedra ROUTE en una escuela de arenisca, con create que devuelve el arg con id 7. */
    private void blockWithSchool() {
        when(schoolBlocks.findById("b1"))
                .thenReturn(Optional.of(block("b1", SchoolBlock.Discipline.ROUTE, null, List.of())));
        when(schools.findById("s1")).thenReturn(Optional.of(
                new School("s1", "Albarracín", null, null, null, "Arenisca", 0, 0, null)));
        when(posts.create(any())).thenAnswer(inv -> withId(inv.getArgument(0), 7L));
    }

    @Test
    void publishSnapshotsRockTypeAndDerivesDisciplineFromBlock() {
        blockWithSchool();

        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, null, null);

        var captor = org.mockito.ArgumentCaptor.forClass(FeedPost.class);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("ROUTE"); // de la piedra
        assertThat(captor.getValue().getRockType()).isEqualTo("Arenisca");
        assertThat(captor.getValue().getSchoolName()).isEqualTo("Albarracín");
    }

    @Test
    void publishPrefersValidClientDisciplineAndIgnoresGarbage() {
        blockWithSchool();

        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, "boulder", null);
        var captor = org.mockito.ArgumentCaptor.forClass(FeedPost.class);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("BOULDER"); // normalizada

        org.mockito.Mockito.clearInvocations(posts);
        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, "SPEED", null);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getDiscipline()).isEqualTo("ROUTE"); // desconocida → piedra
    }

    // ------------------------------------------------------------ caption

    @Test
    void publishSavesCaptionTrimmedAndTruncatedTo500() {
        blockWithSchool();
        String longCaption = "  " + "x".repeat(600) + "  ";

        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, null, longCaption);

        var captor = org.mockito.ArgumentCaptor.forClass(FeedPost.class);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getCaption()).hasSize(500).startsWith("xxx");

        org.mockito.Mockito.clearInvocations(posts);
        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, null, "  ¡Pegue duro!  ");
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getCaption()).isEqualTo("¡Pegue duro!"); // trimmed
    }

    @Test
    void publishBlankCaptionBecomesNull() {
        blockWithSchool();

        publisher.publish("me", "b1", null, FeedViews.KIND_TICK, null, "   ");

        var captor = org.mockito.ArgumentCaptor.forClass(FeedPost.class);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getCaption()).isNull();
    }

    // ------------------------------------------------------------ startType en la vista

    @Test
    void viewExposesStartTypeReadLiveFromLine() {
        FeedPost p = new FeedPost(1L, "ana", null, null, "b1", null, "l1", null, null,
                FeedViews.KIND_TICK, null);
        p.setCaption("brutal");
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(p));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));

        BlockLine line = new BlockLine("l1", "b1", "Vía", "6a",
                BlockLine.StartType.SIT, "[{\"x\":0.1}]", 0, null, 0);
        when(schoolBlocks.findByIds(any()))
                .thenReturn(List.of(block("b1", SchoolBlock.Discipline.BOULDER, null, List.of(line))));

        var result = queryService.page("me", "all", null, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startType()).isEqualTo("SIT");
        assertThat(result.get(0).caption()).isEqualTo("brutal");
    }

    // ------------------------------------------------------------ posts automáticos

    @Test
    void publishSystemCreatesNewBlockPostWithSnapshots() {
        blockWithSchool();

        long id = publisher.publishSystem("author", "b1", null, FeedViews.KIND_NEW_BLOCK);

        assertThat(id).isEqualTo(7L);
        var captor = org.mockito.ArgumentCaptor.forClass(FeedPost.class);
        verify(posts).create(captor.capture());
        assertThat(captor.getValue().getKind()).isEqualTo(FeedViews.KIND_NEW_BLOCK);
        assertThat(captor.getValue().getUserUid()).isEqualTo("author");
        assertThat(captor.getValue().getRockType()).isEqualTo("Arenisca");
    }

    @Test
    void publishSystemRejectsClientKinds() {
        assertThatThrownBy(() -> publisher.publishSystem("author", "b1", null, FeedViews.KIND_TICK))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void newBlockPostExposesCoverFaceLinesOnly() {
        // Post NEW_BLOCK (sin lineId) de una piedra con 3 vías: dos de la cara
        // portada (photoPath null o == portada) y una de OTRA cara.
        FeedPost p = new FeedPost(1L, "ana", null, null, "b1", null, null, null, null,
                FeedViews.KIND_NEW_BLOCK, null);
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(p));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));

        BlockLine cover = new BlockLine("v1", "b1", "Vía portada", "6a", null,
                "[{\"x\":0.1,\"y\":0.9}]", 0, null, 0);   // photoPath null → hereda portada
        BlockLine coverExplicit = new BlockLine("v2", "b1", "Otra", null, null,
                "[{\"x\":0.5,\"y\":0.5}]", 1, "blocks/b1/cover.jpg", 0);
        BlockLine otherFace = new BlockLine("v3", "b1", "Lejos", null, null,
                "[{\"x\":0.2,\"y\":0.2}]", 2, "blocks/b1/otra-cara.jpg", 1);
        when(schoolBlocks.findByIds(any())).thenReturn(List.of(
                block("b1", SchoolBlock.Discipline.BOULDER, "blocks/b1/cover.jpg",
                        List.of(cover, coverExplicit, otherFace))));

        var result = queryService.page("me", "all", null, 20);

        assertThat(result).hasSize(1);
        var lines = result.get(0).blockLines();
        assertThat(lines).hasSize(2); // solo la cara portada
        assertThat(lines.get(0).name()).isEqualTo("Vía portada");
        assertThat(lines.get(0).grade()).isEqualTo("6a");
        assertThat(lines.get(0).linePath()).contains("0.9");
    }

    @Test
    void tickPostDoesNotExposeBlockLines() {
        FeedPost p = post("ana"); // kind TICK, sin lineId
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(p));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));
        BlockLine line = new BlockLine("l1", "b1", "Vía", null, null, "[{\"x\":0.1}]", 0, null, 0);
        when(schoolBlocks.findByIds(any()))
                .thenReturn(List.of(block("b1", SchoolBlock.Discipline.BOULDER, null, List.of(line))));

        var result = queryService.page("me", "all", null, 20);

        assertThat(result.get(0).blockLines()).isNull();
    }

    // ------------------------------------------------------------ delete

    @Test
    void deleteRejectsForeignPostUnlessAdmin() {
        when(posts.findById(1L)).thenReturn(Optional.of(post("ana")));

        assertThatThrownBy(() -> publisher.delete("me", 1L, false))
                .isInstanceOf(ForbiddenException.class);

        publisher.delete("me", 1L, true); // admin sí puede
        verify(posts).deleteById(1L);
    }

    // ------------------------------------------------------------ foto de celebración

    /** MultipartFile con magic bytes de JPEG real. */
    private org.springframework.mock.web.MockMultipartFile jpeg() {
        return new org.springframework.mock.web.MockMultipartFile(
                "file", "celebracion.jpg", "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0});
    }

    @Test
    void uploadPhotoRejectsNonOwner() {
        when(posts.findById(1L)).thenReturn(Optional.of(post("ana")));

        assertThatThrownBy(() -> photoService.uploadPhoto("me", 1L, jpeg()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("solo puedes añadir foto");
    }

    @Test
    void uploadPhotoStoresPathReplacesOldAndReturnsSignedUrl() throws Exception {
        FeedPost p = post("me");
        p.setPhotoPath("feed-photos/1/old.jpg");
        when(posts.findById(1L)).thenReturn(Optional.of(p));
        when(storage.signedReadUrl(any(), anyInt()))
                .thenReturn(java.net.URI.create("https://signed.example/foto").toURL());

        String url = photoService.uploadPhoto("me", 1L, jpeg());

        assertThat(url).isEqualTo("https://signed.example/foto");
        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(posts).updatePhotoPath(eq(1L), pathCaptor.capture());
        assertThat(pathCaptor.getValue()).startsWith("feed-photos/1/").endsWith(".jpg");
        verify(storage).upload(eq(pathCaptor.getValue()), any());
        verify(storage).delete("feed-photos/1/old.jpg"); // la anterior se limpia
    }

    @Test
    void uploadPhotoRejectsNonImageBytes() {
        when(posts.findById(1L)).thenReturn(Optional.of(post("me")));
        var fake = new org.springframework.mock.web.MockMultipartFile(
                "file", "evil.jpg", "image/jpeg", "MZ ejecutable".getBytes());

        assertThatThrownBy(() -> photoService.uploadPhoto("me", 1L, fake))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    @Test
    void viewPhotoUrlIsNullWithoutPhotoAndSignedWithPhoto() throws Exception {
        FeedPost noPhoto = post("ana");
        FeedPost withPhoto = withId(post("ana"), 2L);
        withPhoto.setPhotoPath("feed-photos/2/x.jpg");
        when(posts.pageAllPublic(anyLong(), anyInt())).thenReturn(List.of(noPhoto, withPhoto));
        User ana = user("ana", true);
        when(users.findByUids(anyList())).thenReturn(List.of(ana));
        when(mapper.toPublic(ana)).thenReturn(profile("ana", "ana"));
        when(storage.signedReadUrl(eq("feed-photos/2/x.jpg"), anyInt()))
                .thenReturn(java.net.URI.create("https://signed.example/x").toURL());

        var result = queryService.page("me", "all", null, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).photoUrl()).isNull();
        assertThat(result.get(1).photoUrl()).isEqualTo("https://signed.example/x");
    }

    @Test
    void deleteRemovesPhotoFromStorageAndSurvivesStorageFailure() {
        FeedPost p = post("me");
        p.setPhotoPath("feed-photos/1/x.jpg");
        when(posts.findById(1L)).thenReturn(Optional.of(p));
        org.mockito.Mockito.doThrow(new RuntimeException("storage caído"))
                .when(storage).delete("feed-photos/1/x.jpg");

        publisher.delete("me", 1L, false); // no lanza pese al fallo del Storage

        verify(posts).deleteById(1L);
        verify(storage).delete("feed-photos/1/x.jpg");
    }
}
