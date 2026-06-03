package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.journal.JournalDtos;
import com.meteomontana.api.application.journal.JournalUseCase;
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

@RestController
@RequestMapping("/api")
public class JournalController {

    private final JournalUseCase useCase;

    public JournalController(JournalUseCase useCase) {
        this.useCase = useCase;
    }

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

    @DeleteMapping("/journal/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user, @PathVariable String id) {
        useCase.delete(user.uid(), id);
    }
}
