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
            boolean theyFollowMe,
            boolean requestPending
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
        // Si ya existe la relación (aceptada o pendiente), no hacemos nada.
        if (followRepository.isFollowing(followerUid, followedUid)) return;
        if (followRepository.hasPendingRequest(followerUid, followedUid)) return;

        boolean targetIsPublic = target.isPublic();
        String status = targetIsPublic ? "ACCEPTED" : "PENDING";
        followRepository.add(followerUid, followedUid, status);

        User me = userRepository.findByUid(followerUid).orElse(null);
        String myName = me != null
                ? (me.getUsername() != null ? "@" + me.getUsername() : me.getDisplayName())
                : "Alguien";

        if (targetIsPublic) {
            notificationService.create(
                    followedUid, "NEW_FOLLOWER",
                    "Nuevo seguidor",
                    myName + " te ha empezado a seguir",
                    "user", followerUid
            );
            if (target.getFcmToken() != null) {
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
        } else {
            // Perfil privado → solicitud pendiente.
            notificationService.create(
                    followedUid, "FOLLOW_REQUEST",
                    "Solicitud de seguimiento",
                    myName + " quiere seguirte",
                    "follow_request", followerUid
            );
            if (target.getFcmToken() != null) {
                fcmService.sendToToken(
                        target.getFcmToken(),
                        myName + " quiere seguirte",
                        "Pulsa para aceptar o rechazar",
                        Map.of(
                            "targetType", "follow_request",
                            "targetId", followerUid,
                            "title", myName + " quiere seguirte",
                            "body", "Pulsa para aceptar o rechazar"
                        )
                );
            }
        }
    }

    @Transactional
    public void unfollow(String followerUid, String followedUid) {
        followRepository.remove(followerUid, followedUid);
    }

    @Transactional
    public void acceptRequest(String myUid, String requesterUid) {
        if (!followRepository.hasPendingRequest(requesterUid, myUid)) {
            throw new UserNotFoundException(requesterUid);
        }
        followRepository.acceptRequest(requesterUid, myUid);

        User me = userRepository.findByUid(myUid).orElse(null);
        String myName = me != null
                ? (me.getUsername() != null ? "@" + me.getUsername() : me.getDisplayName())
                : "Alguien";
        User requester = userRepository.findByUid(requesterUid).orElse(null);

        notificationService.create(
                requesterUid, "FOLLOW_ACCEPTED",
                "Solicitud aceptada",
                myName + " ha aceptado tu solicitud",
                "user", myUid
        );
        if (requester != null && requester.getFcmToken() != null) {
            fcmService.sendToToken(
                    requester.getFcmToken(),
                    myName + " ha aceptado tu solicitud",
                    "Ya puedes ver su perfil",
                    Map.of(
                        "targetType", "user",
                        "targetId", myUid,
                        "title", myName + " ha aceptado tu solicitud",
                        "body", "Ya puedes ver su perfil"
                    )
            );
        }
    }

    @Transactional
    public void rejectRequest(String myUid, String requesterUid) {
        followRepository.remove(requesterUid, myUid);
    }

    public FollowStatusDto statusFor(String currentUid, String targetUid) {
        boolean iFollowThem = currentUid != null && followRepository.isFollowing(currentUid, targetUid);
        boolean theyFollowMe = currentUid != null && followRepository.isFollowing(targetUid, currentUid);
        boolean pending = currentUid != null && followRepository.hasPendingRequest(currentUid, targetUid);
        return new FollowStatusDto(
                followRepository.countFollowers(targetUid),
                followRepository.countFollowing(targetUid),
                iFollowThem,
                theyFollowMe,
                pending
        );
    }

    public List<PublicProfileDto> listFollowers(String uid) {
        return resolveProfiles(followRepository.followersOf(uid));
    }

    public List<PublicProfileDto> listFollowing(String uid) {
        return resolveProfiles(followRepository.followingOf(uid));
    }

    public List<PublicProfileDto> listPendingRequests(String uid) {
        return resolveProfiles(followRepository.pendingRequestsFor(uid));
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
