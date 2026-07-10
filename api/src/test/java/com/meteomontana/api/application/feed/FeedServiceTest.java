package com.meteomontana.api.application.feed;

import com.meteomontana.api.application.moderation.UserModerationService;
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
import com.meteomontana.api.infrastructure.persistence.jpa.UserBlockJpaEntity;
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

    FeedService service;

    @BeforeEach
    void setUp() {
        service = new FeedService(posts, likes, comments, follows, blocks,
                schoolBlocks, schools, users, mapper, moderation);
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
