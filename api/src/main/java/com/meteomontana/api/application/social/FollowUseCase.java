package com.meteomontana.api.application.social;

import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.UserNotFoundException;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.FollowRepository;
import com.meteomontana.api.domain.port.UserRepository;
import com.meteomontana.api.infrastructure.push.FcmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FollowUseCase {

    public record FollowStatusDto(
            long followers,
            long following,
            boolean iFollowThem,
            boolean theyFollowMe
    ) {}

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final UserDtoMapper userDtoMapper;

    public FollowUseCase(FollowRepository followRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         FcmService fcmService,
                         UserDtoMapper userDtoMapper) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.fcmService = fcmService;
        this.userDtoMapper = userDtoMapper;
    }

    @Transactional
    public void follow(String followerUid, String followedUid) {
        if (followerUid.equals(followedUid)) throw new ForbiddenException("No puedes seguirte a ti mismo");
        User target = userRepository.findByUid(followedUid)
                .orElseThrow(() -> new UserNotFoundException(followedUid));
        if (followRepository.isFollowing(followerUid, followedUid)) return;

        followRepository.add(followerUid, followedUid);

        // Notificación + push al seguido
        User me = userRepository.findByUid(followerUid).orElse(null);
        String myName = me != null
                ? (me.getUsername() != null ? "@" + me.getUsername() : me.getDisplayName())
                : "Alguien";
        notificationService.create(
                followedUid, "NEW_FOLLOWER",
                "Nuevo seguidor",
                myName + " te ha empezado a seguir",
                "user", followerUid
        );
        if (target.getFcmToken() != null) {
            // Data payload con targetType/Id para que el cliente Android haga deep link
            // al perfil público del seguidor al pulsar la notificación.
            fcmService.sendToToken(
                    target.getFcmToken(),
                    myName + " te sigue ahora",
                    "Pulsa para ver su perfil",
                    Map.of(
                        "targetType", "user",
                        "targetId", followerUid,
                        "title", myName + " te sigue ahora",
                        "body", "Pulsa para ver su perfil"
                    )
            );
        }
    }

    @Transactional
    public void unfollow(String followerUid, String followedUid) {
        followRepository.remove(followerUid, followedUid);
    }

    public FollowStatusDto statusFor(String currentUid, String targetUid) {
        return new FollowStatusDto(
                followRepository.countFollowers(targetUid),
                followRepository.countFollowing(targetUid),
                followRepository.isFollowing(currentUid, targetUid),
                followRepository.isFollowing(targetUid, currentUid)
        );
    }

    public List<PublicProfileDto> listFollowers(String uid) {
        return resolveProfiles(followRepository.followersOf(uid));
    }

    public List<PublicProfileDto> listFollowing(String uid) {
        return resolveProfiles(followRepository.followingOf(uid));
    }

    private List<PublicProfileDto> resolveProfiles(List<String> uids) {
        return uids.stream()
                .map(userRepository::findByUid)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(userDtoMapper::toPublic)
                .toList();
    }
}
