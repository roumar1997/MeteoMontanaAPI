package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.users.GetOrCreateMyProfileUseCase;
import com.meteomontana.api.application.users.GetPublicProfileUseCase;
import com.meteomontana.api.application.users.PrivateProfileDto;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UpdateFcmTokenUseCase;
import com.meteomontana.api.application.users.UpdateMyProfileUseCase;
import com.meteomontana.api.application.users.UpdateProfileRequest;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final GetOrCreateMyProfileUseCase getOrCreateMyProfile;
    private final GetPublicProfileUseCase     getPublicProfile;
    private final UpdateMyProfileUseCase      updateMyProfile;
    private final UpdateFcmTokenUseCase       updateFcmToken;

    public UserController(GetOrCreateMyProfileUseCase getOrCreateMyProfile,
                          GetPublicProfileUseCase getPublicProfile,
                          UpdateMyProfileUseCase updateMyProfile,
                          UpdateFcmTokenUseCase updateFcmToken) {
        this.getOrCreateMyProfile = getOrCreateMyProfile;
        this.getPublicProfile     = getPublicProfile;
        this.updateMyProfile      = updateMyProfile;
        this.updateFcmToken       = updateFcmToken;
    }

    @GetMapping("/me")
    public PrivateProfileDto getMe(@AuthenticationPrincipal FirebaseUser user) {
        return getOrCreateMyProfile.execute(user);
    }

    @PutMapping("/me")
    public PrivateProfileDto updateMe(@AuthenticationPrincipal FirebaseUser user,
                                      @RequestBody UpdateProfileRequest request) {
        return updateMyProfile.execute(user.uid(), request);
    }

    @PutMapping("/me/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateFcmToken(@AuthenticationPrincipal FirebaseUser user,
                               @RequestBody UpdateFcmTokenUseCase.FcmTokenRequest req) {
        updateFcmToken.execute(user.uid(), req.token());
    }

    @GetMapping("/users/{identifier}")
    public PublicProfileDto getPublic(@PathVariable String identifier) {
        return getPublicProfile.execute(identifier);
    }
}
