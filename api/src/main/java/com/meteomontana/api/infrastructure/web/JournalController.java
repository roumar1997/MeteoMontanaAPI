package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.journal.JournalDtos;
import com.meteomontana.api.application.journal.JournalUseCase;
import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JournalController {

    private final JournalUseCase useCase;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @PostMapping("/journal")
    @ResponseStatus(HttpStatus.CREATED)
    public JournalDtos.JournalSessionDto create(
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody JournalDtos.CreateJournalRequest req) {
        return useCase.create(user.uid(), req);
    }

    @GetMapping("/journal/me")
    public List<JournalDtos.JournalSessionDto> mine(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.listMine(user.uid());
    }

    @GetMapping("/journal/me/stats")
    public JournalDtos.JournalStatsDto myStats(@AuthenticationPrincipal FirebaseUser user) {
        return useCase.statsFor(user.uid());
    }

    /** C3: cambiar la fecha de una entrada ("la hice el 12 de abril"). */
    public record UpdateDateRequest(java.time.LocalDate date) {}

    @org.springframework.web.bind.annotation.PatchMapping("/journal/{id}/date")
    public JournalDtos.JournalSessionDto updateDate(
            @org.springframework.web.bind.annotation.PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody UpdateDateRequest req,
            @AuthenticationPrincipal FirebaseUser user) {
        return useCase.updateDate(user.uid(), id, req.date());
    }

    @DeleteMapping("/journal/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        useCase.delete(user.uid(), id);
    }

    /**
     * Stats públicas de otro usuario (bloques, escuelas, máximo grado).
     * Devuelve si el usuario es público, es el propio caller, o el caller es
     * seguidor aceptado. Si no → 404 (perfil privado).
     */
    @GetMapping("/users/{identifier}/stats")
    public JournalDtos.JournalStatsDto userStats(@PathVariable String identifier,
                                                 @AuthenticationPrincipal FirebaseUser caller) {
        User user = resolveAndCheckAccess(identifier, caller);
        return useCase.statsFor(user.getUid());
    }

    /**
     * Diario público de otro usuario. Misma regla de privacidad que /stats.
     */
    @GetMapping("/users/{identifier}/journal")
    public List<JournalDtos.JournalSessionDto> userJournal(@PathVariable String identifier,
                                                           @AuthenticationPrincipal FirebaseUser caller) {
        User user = resolveAndCheckAccess(identifier, caller);
        return useCase.listMine(user.getUid());
    }

    private User resolveAndCheckAccess(String identifier, FirebaseUser caller) {
        User user = userRepository.findByUid(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UserNotFoundException(identifier));
        String callerUid = (caller != null) ? caller.uid() : null;
        boolean isSelf = callerUid != null && callerUid.equals(user.getUid());
        boolean isAcceptedFollower = callerUid != null
                && followRepository.isFollowing(callerUid, user.getUid());
        if (!user.isPublic() && !isSelf && !isAcceptedFollower) {
            throw new UserNotFoundException(identifier);
        }
        return user;
    }
}
