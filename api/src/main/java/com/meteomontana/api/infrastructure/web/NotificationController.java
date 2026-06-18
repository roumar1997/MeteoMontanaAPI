package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.social.NotificationDtos;
import com.meteomontana.api.application.social.NotificationInboxUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final NotificationInboxUseCase useCase;

    public NotificationController(NotificationInboxUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public NotificationDtos.InboxDto inbox(@AuthenticationPrincipal FirebaseUser me,
                                           @RequestParam(defaultValue = "50") int limit) {
        return useCase.inbox(me.uid(), limit);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable String id) {
        useCase.markRead(id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal FirebaseUser me) {
        useCase.markAllRead(me.uid());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser me, @PathVariable String id) {
        useCase.delete(id, me.uid());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll(@AuthenticationPrincipal FirebaseUser me) {
        useCase.deleteAll(me.uid());
    }
}
