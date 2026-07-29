package com.meteomontana.api.application.contribution;

import com.meteomontana.api.application.feed.FeedPublishService;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.infrastructure.persistence.SpringDataContributionRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.PendingContributionJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Posts automáticos del feed al aprobar contribuciones. Lo crítico: crear el
 * post NUNCA puede tumbar la aprobación (try/catch en publishFeedPost).
 */
class ReviewContributionUseCaseTest {

    SpringDataContributionRepository repo = mock(SpringDataContributionRepository.class);
    SpringDataSchoolBlockRepository blockRepo = mock(SpringDataSchoolBlockRepository.class);
    SpringDataSchoolRepository schoolRepo = mock(SpringDataSchoolRepository.class);
    com.meteomontana.api.infrastructure.email.ResendEmailService emailService =
            mock(com.meteomontana.api.infrastructure.email.ResendEmailService.class);
    com.meteomontana.api.domain.port.UserRepository userRepository =
            mock(com.meteomontana.api.domain.port.UserRepository.class);
    com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository journalRepo =
            mock(com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository.class);
    FeedPublishService feedService = mock(FeedPublishService.class);
    com.meteomontana.api.domain.port.CommunityVoteRepository communityVotes =
            mock(com.meteomontana.api.domain.port.CommunityVoteRepository.class);

    ReviewContributionUseCase useCase;

    FirebaseUser admin = new FirebaseUser("admin-uid", "admin@x.com", "Admin");

    @BeforeEach
    void setUp() {
        // Colaboradores REALES cableados con los mocks de siempre (mismo patron
        // que los tests del FeedService troceado).
        useCase = new ReviewContributionUseCase(repo, blockRepo, schoolRepo,
                new BlockMaterializer(blockRepo, communityVotes),
                new LineReconciler(blockRepo, journalRepo),
                new ReviewNotifier(emailService, userRepository),
                feedService);
    }

    /** Contribución BOULDER pendiente de piedra NUEVA (sin targetBlockId). */
    private PendingContributionJpaEntity pendingNewBoulder() {
        PendingContribution c = mock(PendingContribution.class);
        when(c.getType()).thenReturn(PendingContribution.Type.BOULDER);
        when(c.getStatus()).thenReturn(SubmissionStatus.APPROVED); // para ContributionResponse.from
        when(c.getSchoolId()).thenReturn("s1");
        when(c.getSubmittedByUid()).thenReturn("author-uid");
        when(c.getId()).thenReturn("c1");

        PendingContributionJpaEntity entity = mock(PendingContributionJpaEntity.class);
        when(entity.getStatus()).thenReturn(SubmissionStatus.PENDING);
        when(entity.toDomain()).thenReturn(c);
        when(repo.findById("c1")).thenReturn(Optional.of(entity));

        when(blockRepo.findBySchoolIdOrderByCreatedAtAsc("s1")).thenReturn(List.of());
        when(blockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return entity;
    }

    @Test
    void approvingNewBoulderPublishesNewBlockPostWithContributionAuthor() {
        pendingNewBoulder();

        useCase.approve("c1", admin, false);

        // publishSystem ahora recibe IDS (frontera limpia con el feed).
        verify(feedService).publishSystem(eq("author-uid"), any(String.class),
                isNull(), eq(com.meteomontana.api.application.feed.FeedViews.KIND_NEW_BLOCK));
    }

    @Test
    void feedPostFailureNeverBreaksTheApproval() {
        PendingContributionJpaEntity entity = pendingNewBoulder();
        when(feedService.publishSystem(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("feed caído"));

        assertThatCode(() -> useCase.approve("c1", admin, false))
                .doesNotThrowAnyException();

        // La aprobación se completó igualmente.
        verify(entity).setStatus(SubmissionStatus.APPROVED);
        verify(repo).save(entity);
        // Y la piedra se materializó.
        var captor = org.mockito.ArgumentCaptor.forClass(SchoolBlockJpaEntity.class);
        verify(blockRepo).save(captor.capture());
        assertThat(captor.getValue().getSchoolId()).isEqualTo("s1");
    }
}
