package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GetMeetupsUseCaseTest {

    MeetupRepository meetupRepository = mock(MeetupRepository.class);
    FollowRepository followRepository = mock(FollowRepository.class);
    UserRepository   userRepository   = mock(UserRepository.class);
    SchoolRepository schoolRepository = mock(SchoolRepository.class);

    GetMeetupsUseCase useCase;

    private static final String CREATOR = "uid-creator";
    private static final String REQUESTER = "uid-requester";

    @BeforeEach void setUp() {
        MeetupDtoMapper mapper = new MeetupDtoMapper(schoolRepository, userRepository);
        useCase = new GetMeetupsUseCase(meetupRepository, followRepository, userRepository, mapper);
        when(schoolRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.findByUid(any())).thenReturn(Optional.empty());
    }

    private Meetup meetup(String privacy) {
        LocalDate day = LocalDate.now().plusDays(1);
        return new Meetup("id-1", "school-1", "Test", null, null, privacy, null, null,
                CREATOR, "conv-1", List.of(day), day,
                day.plusDays(1).atStartOfDay(), LocalDateTime.now(), List.of());
    }

    @Test void open_meetup_visible_to_anyone() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("OPEN")));
        var result = useCase.execute(REQUESTER, null, null, null);
        assertThat(result).hasSize(1);
    }

    @Test void followers_meetup_hidden_if_not_following() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("FOLLOWERS")));
        when(followRepository.isFollowing(REQUESTER, CREATOR)).thenReturn(false);
        var result = useCase.execute(REQUESTER, null, null, null);
        assertThat(result).isEmpty();
    }

    @Test void followers_meetup_visible_if_following() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("FOLLOWERS")));
        when(followRepository.isFollowing(REQUESTER, CREATOR)).thenReturn(true);
        when(userRepository.findByUid(REQUESTER)).thenReturn(Optional.of(
                new User(REQUESTER, "", null, null, null, null, true, null,
                         false, false, null, null, null, null)));
        var result = useCase.execute(REQUESTER, null, null, null);
        assertThat(result).hasSize(1);
    }

    @Test void women_meetup_hidden_if_not_woman() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("WOMEN")));
        when(userRepository.findByUid(REQUESTER)).thenReturn(Optional.of(
                new User(REQUESTER, "", null, null, null, null, true, null,
                         false, false, null, "MAN", null, null)));
        var result = useCase.execute(REQUESTER, null, null, null);
        assertThat(result).isEmpty();
    }

    @Test void women_meetup_visible_if_woman() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("WOMEN")));
        when(userRepository.findByUid(REQUESTER)).thenReturn(Optional.of(
                new User(REQUESTER, "", null, null, null, null, true, null,
                         false, false, null, "WOMAN", null, null)));
        var result = useCase.execute(REQUESTER, null, null, null);
        assertThat(result).hasSize(1);
    }

    @Test void filter_by_school_id() {
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("OPEN")));
        var result = useCase.execute(REQUESTER, "other-school", null, null);
        assertThat(result).isEmpty();
    }

    @Test void filter_by_date_matches() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("OPEN")));
        var result = useCase.execute(REQUESTER, null, tomorrow, null);
        assertThat(result).hasSize(1);
    }

    @Test void filter_by_date_no_match() {
        LocalDate nextWeek = LocalDate.now().plusDays(10);
        when(meetupRepository.findActive()).thenReturn(List.of(meetup("OPEN")));
        var result = useCase.execute(REQUESTER, null, nextWeek, null);
        assertThat(result).isEmpty();
    }
}
