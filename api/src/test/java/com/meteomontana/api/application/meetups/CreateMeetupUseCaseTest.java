package com.meteomontana.api.application.meetups;

import com.meteomontana.api.application.social.NotificationService;
import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateMeetupUseCaseTest {

    MeetupRepository     meetupRepository     = mock(MeetupRepository.class);
    ChatRepository       chatRepository       = mock(ChatRepository.class);
    SchoolRepository     schoolRepository     = mock(SchoolRepository.class);
    UserRepository       userRepository       = mock(UserRepository.class);
    MeetupAlertRepository alertRepository     = mock(MeetupAlertRepository.class);
    FollowRepository     followRepository     = mock(FollowRepository.class);
    NotificationService  notificationService  = mock(NotificationService.class);
    PushSender           pushSender           = mock(PushSender.class);
    com.meteomontana.api.application.moderation.UserModerationService moderation =
            mock(com.meteomontana.api.application.moderation.UserModerationService.class);

    CreateMeetupUseCase useCase;

    private static final String CREATOR = "uid-creator";

    @BeforeEach void setUp() {
        MeetupDtoMapper mapper = new MeetupDtoMapper(schoolRepository, userRepository);
        useCase = new CreateMeetupUseCase(meetupRepository, chatRepository, schoolRepository,
                userRepository, alertRepository, followRepository, notificationService, pushSender, mapper,
                moderation);

        when(schoolRepository.findById("school-1")).thenReturn(Optional.of(
                new School("school-1", "La Muela", null, null, null, null, 0, 0, null)));
        when(chatRepository.createGroup(any(), any(), any())).thenReturn("conv-123");
        when(meetupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User user(String gender) {
        return new User(CREATOR, "a@b.com", "user1", "User One", null, null,
                true, null, false, false, null, gender, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test void creates_open_meetup_successfully() {
        when(userRepository.findByUid(CREATOR)).thenReturn(Optional.of(user(null)));
        var req = new CreateMeetupRequest("school-1", "Quedada verano", null, "BOULDER",
                "OPEN", null, null, List.of(LocalDate.now().plusDays(1)));
        var result = useCase.execute(CREATOR, req);
        assertThat(result).isNotNull();
        verify(chatRepository).createGroup(eq(CREATOR), any(), eq(List.of()));
        verify(meetupRepository).save(any());
    }

    @Test void rejects_women_meetup_if_creator_not_woman() {
        when(userRepository.findByUid(CREATOR)).thenReturn(Optional.of(user("MAN")));
        var req = new CreateMeetupRequest("school-1", "Solo chicas", null, null,
                "WOMEN", null, null, List.of(LocalDate.now().plusDays(1)));
        assertThatThrownBy(() -> useCase.execute(CREATOR, req))
                .isInstanceOf(com.meteomontana.api.domain.exception.ForbiddenException.class)
                .hasMessage("GENDER_REQUIRED");
    }

    @Test void allows_women_meetup_if_creator_is_woman() {
        when(userRepository.findByUid(CREATOR)).thenReturn(Optional.of(user("WOMAN")));
        var req = new CreateMeetupRequest("school-1", "Solo chicas", null, null,
                "WOMEN", null, null, List.of(LocalDate.now().plusDays(1)));
        var result = useCase.execute(CREATOR, req);
        assertThat(result).isNotNull();
    }

    @Test void rejects_empty_days() {
        when(userRepository.findByUid(CREATOR)).thenReturn(Optional.of(user(null)));
        var req = new CreateMeetupRequest("school-1", "Test", null, null, "OPEN", null, null, List.of());
        assertThatThrownBy(() -> useCase.execute(CREATOR, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void discipline_both_is_accepted() {
        when(userRepository.findByUid(CREATOR)).thenReturn(Optional.of(user(null)));
        var req = new CreateMeetupRequest("school-1", "Polivalente", null, "BOTH",
                "OPEN", null, null, List.of(LocalDate.now().plusDays(1)));
        var result = useCase.execute(CREATOR, req);
        assertThat(result).isNotNull();
    }
}
