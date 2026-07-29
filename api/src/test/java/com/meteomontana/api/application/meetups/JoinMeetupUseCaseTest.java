package com.meteomontana.api.application.meetups;

import com.meteomontana.api.domain.exception.ConflictException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.model.Meetup;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ChatRepository;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.MeetupRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Control de acceso al unirse a una quedada (zona caliente, sin test hasta ahora).
 * Matriz clave: `invited` (enlace de invitación) salta FOLLOWERS pero NUNCA WOMEN;
 * más aforo (MEETUP_FULL) e idempotencia (ya miembro).
 */
class JoinMeetupUseCaseTest {

    MeetupRepository meetupRepository = mock(MeetupRepository.class);
    ChatRepository   chatRepository   = mock(ChatRepository.class);
    FollowRepository followRepository = mock(FollowRepository.class);
    UserRepository   userRepository   = mock(UserRepository.class);
    MeetupDtoMapper  mapper           = mock(MeetupDtoMapper.class);

    JoinMeetupUseCase useCase;

    private static final String CREATOR = "uid-creator";
    private static final String JOINER  = "uid-joiner";

    @BeforeEach void setUp() {
        useCase = new JoinMeetupUseCase(meetupRepository, chatRepository, followRepository, userRepository, mapper);
        when(mapper.toDto(any(), any())).thenReturn(null);
        when(chatRepository.participantsOf(any())).thenReturn(List.of(CREATOR));
        when(meetupRepository.isMember(any(), any())).thenReturn(false);
    }

    private Meetup meetup(String privacy, Integer limit, int memberCount) {
        List<Meetup.MeetupMember> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++)
            members.add(new Meetup.MeetupMember("m" + i, null, null, null, LocalDateTime.now(), null));
        Meetup m = new Meetup("m1", "school-1", "Q", null, "BOULDER", privacy, limit, null,
                CREATOR, "conv-1", List.of(LocalDate.now().plusDays(1)),
                LocalDate.now().plusDays(1), LocalDateTime.now(), LocalDateTime.now(), members);
        when(meetupRepository.findById("m1")).thenReturn(Optional.of(m));
        return m;
    }

    private User user(String gender) {
        return new User(JOINER, "j@b.com", "joiner", "Joiner", null, null,
                true, null, false, false, null, gender, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test void ya_miembro_no_vuelve_a_anadir() {
        meetup("OPEN", null, 1);
        when(meetupRepository.isMember("m1", JOINER)).thenReturn(true);
        useCase.execute(JOINER, "m1", false);
        verify(meetupRepository, never()).addMember(any(), any());
    }

    @Test void followers_sin_invitacion_ni_seguir_rechaza() {
        meetup("FOLLOWERS", null, 0);
        when(followRepository.isFollowing(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> useCase.execute(JOINER, "m1", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("FOLLOW_REQUIRED");
        verify(meetupRepository, never()).addMember(any(), any());
    }

    @Test void followers_con_invitacion_entra() {
        meetup("FOLLOWERS", null, 0);
        useCase.execute(JOINER, "m1", true);
        verify(meetupRepository).addMember("m1", JOINER);
    }

    @Test void followers_siguiendo_al_creador_entra() {
        meetup("FOLLOWERS", null, 0);
        when(followRepository.isFollowing(JOINER, CREATOR)).thenReturn(true);
        useCase.execute(JOINER, "m1", false);
        verify(meetupRepository).addMember("m1", JOINER);
    }

    @Test void women_con_invitacion_pero_no_mujer_sigue_rechazando() {
        meetup("WOMEN", null, 0);
        when(userRepository.findByUid(JOINER)).thenReturn(Optional.of(user("MAN")));
        assertThatThrownBy(() -> useCase.execute(JOINER, "m1", true))   // invited NO salta género
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("GENDER_REQUIRED");
        verify(meetupRepository, never()).addMember(any(), any());
    }

    @Test void women_siendo_mujer_entra() {
        meetup("WOMEN", null, 0);
        when(userRepository.findByUid(JOINER)).thenReturn(Optional.of(user("WOMAN")));
        useCase.execute(JOINER, "m1", false);
        verify(meetupRepository).addMember("m1", JOINER);
    }

    @Test void aforo_completo_rechaza() {
        meetup("OPEN", 2, 2);   // limit 2, ya hay 2 → lleno
        assertThatThrownBy(() -> useCase.execute(JOINER, "m1", false))
                .isInstanceOf(ConflictException.class)
                .hasMessage("MEETUP_FULL");
        verify(meetupRepository, never()).addMember(any(), any());
    }

    @Test void abierta_entra_y_actualiza_participantes() {
        meetup("OPEN", null, 0);
        useCase.execute(JOINER, "m1", false);
        verify(meetupRepository).addMember("m1", JOINER);
        verify(chatRepository).updateParticipants(eq("conv-1"), any());
    }
}
