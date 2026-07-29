package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.users.DeleteMyAccountUseCase;
import com.meteomontana.api.application.users.GetOrCreateMyProfileUseCase;
import com.meteomontana.api.application.users.GetPublicProfileUseCase;
import com.meteomontana.api.application.users.PrivateProfileDto;
import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UpdateFcmTokenUseCase;
import com.meteomontana.api.application.users.UpdateMyProfileUseCase;
import com.meteomontana.api.application.users.UpdateProfilePhotoUseCase;
import com.meteomontana.api.application.users.UpdateProfileRequest;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final GetOrCreateMyProfileUseCase getOrCreateMyProfile;
    private final GetPublicProfileUseCase     getPublicProfile;
    private final UpdateMyProfileUseCase      updateMyProfile;
    private final UpdateFcmTokenUseCase       updateFcmToken;
    private final UpdateProfilePhotoUseCase   updateProfilePhoto;
    private final DeleteMyAccountUseCase      deleteMyAccount;
    private final UserDtoMapper               userDtoMapper;

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

    /** Borrado de cuenta (requisito de las tiendas): elimina datos + Firebase Auth. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal FirebaseUser user) {
        deleteMyAccount.execute(user.uid());
    }

    @PostMapping("/me/photo")
    public PrivateProfileDto updateMyPhoto(@AuthenticationPrincipal FirebaseUser user,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        return updateProfilePhoto.execute(user.uid(), file, userDtoMapper);
    }

    @GetMapping("/users/{identifier}")
    public PublicProfileDto getPublic(@PathVariable String identifier,
                                      @AuthenticationPrincipal FirebaseUser user) {
        String currentUid = (user != null) ? user.uid() : null;
        return getPublicProfile.execute(identifier, currentUid);
    }
}
