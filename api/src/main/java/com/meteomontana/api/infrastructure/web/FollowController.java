package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.social.FollowUseCase;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FollowController {

    private final FollowUseCase useCase;

    @PostMapping("/users/{uid}/follow")
    @ResponseStatus(HttpStatus.CREATED)
    public void follow(@AuthenticationPrincipal FirebaseUser me, @PathVariable String uid) {
        useCase.follow(me.uid(), uid);
    }

    @DeleteMapping("/users/{uid}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@AuthenticationPrincipal FirebaseUser me, @PathVariable String uid) {
        useCase.unfollow(me.uid(), uid);
    }

    @GetMapping("/users/{uid}/follow-status")
    public FollowUseCase.FollowStatusDto status(@AuthenticationPrincipal FirebaseUser me,
                                                @PathVariable String uid) {
        return useCase.statusFor(me.uid(), uid);
    }

    @GetMapping("/users/{uid}/followers")
    public List<PublicProfileDto> followers(@PathVariable String uid) {
        return useCase.listFollowers(uid);
    }

    @GetMapping("/users/{uid}/following")
    public List<PublicProfileDto> following(@PathVariable String uid) {
        return useCase.listFollowing(uid);
    }

    @DeleteMapping("/me/followers/{followerUid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFollower(@AuthenticationPrincipal FirebaseUser me, @PathVariable String followerUid) {
        useCase.removeFollower(me.uid(), followerUid);
    }

    @GetMapping("/me/follow-requests")
    public List<PublicProfileDto> myPendingRequests(@AuthenticationPrincipal FirebaseUser me) {
        return useCase.listPendingRequests(me.uid());
    }

    @PostMapping("/me/follow-requests/{requesterUid}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptRequest(@AuthenticationPrincipal FirebaseUser me, @PathVariable String requesterUid) {
        useCase.acceptRequest(me.uid(), requesterUid);
    }

    @PostMapping("/me/follow-requests/{requesterUid}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@AuthenticationPrincipal FirebaseUser me, @PathVariable String requesterUid) {
        useCase.rejectRequest(me.uid(), requesterUid);
    }
}
