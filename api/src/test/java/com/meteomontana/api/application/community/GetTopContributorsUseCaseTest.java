package com.meteomontana.api.application.community;

import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ContributionStatsRepository;
import com.meteomontana.api.domain.port.ContributionStatsRepository.ContributorCount;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GetTopContributorsUseCaseTest {

    ContributionStatsRepository statsRepo = mock(ContributionStatsRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    UserDtoMapper mapper = mock(UserDtoMapper.class);

    GetTopContributorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTopContributorsUseCase(statsRepo, userRepository, mapper);
    }

    private User user(String uid, boolean isPublic) {
        User u = mock(User.class);
        when(u.getUid()).thenReturn(uid);
        when(u.isPublic()).thenReturn(isPublic);
        return u;
    }

    private PublicProfileDto profile(String uid, String username) {
        return new PublicProfileDto(uid, username, "Name " + username,
                "https://photo/" + username, null, null, false, true);
    }

    @Test
    void ranksByApprovedCountAndKeepsOrder() {
        when(statsRepo.topContributors(eq(SubmissionStatus.APPROVED), anyInt()))
                .thenReturn(List.of(
                        new ContributorCount("u1", 12),
                        new ContributorCount("u2", 5)));
        User u1 = user("u1", true);
        User u2 = user("u2", true);
        when(userRepository.findByUids(any())).thenReturn(List.of(u2, u1)); // desordenados a propósito
        when(mapper.toPublic(u1)).thenReturn(profile("u1", "ana"));
        when(mapper.toPublic(u2)).thenReturn(profile("u2", "bea"));

        var result = useCase.topContributors(20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).username()).isEqualTo("ana");   // 12 aprobadas primero
        assertThat(result.get(0).approvedCount()).isEqualTo(12);
        assertThat(result.get(1).username()).isEqualTo("bea");
    }

    @Test
    void privateProfilesUseLockedView() {
        when(statsRepo.topContributors(eq(SubmissionStatus.APPROVED), anyInt()))
                .thenReturn(List.of(new ContributorCount("u1", 3)));
        User u1 = user("u1", false);
        when(userRepository.findByUids(any())).thenReturn(List.of(u1));
        when(mapper.toPublicLocked(u1)).thenReturn(profile("u1", "carla"));

        var result = useCase.topContributors(20);

        assertThat(result).hasSize(1);
        verify(mapper).toPublicLocked(u1);
        verify(mapper, never()).toPublic(any());
    }

    @Test
    void deletedUsersAreDroppedFromRanking() {
        when(statsRepo.topContributors(eq(SubmissionStatus.APPROVED), anyInt()))
                .thenReturn(List.of(
                        new ContributorCount("ghost", 9),
                        new ContributorCount("u1", 2)));
        User u1 = user("u1", true);
        when(userRepository.findByUids(any())).thenReturn(List.of(u1)); // "ghost" ya no existe
        when(mapper.toPublic(u1)).thenReturn(profile("u1", "dani"));

        var result = useCase.topContributors(20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("dani");
    }

    @Test
    void limitIsCappedAt50() {
        when(statsRepo.topContributors(any(), anyInt())).thenReturn(List.of());

        useCase.topContributors(9999);

        verify(statsRepo).topContributors(SubmissionStatus.APPROVED, 50);
    }
}
