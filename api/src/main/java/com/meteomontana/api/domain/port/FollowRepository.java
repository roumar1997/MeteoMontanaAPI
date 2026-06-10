package com.meteomontana.api.domain.port;

import java.util.List;

public interface FollowRepository {

    /** Crea o sobrescribe la relación con el status indicado (ACCEPTED / PENDING). */
    void add(String followerUid, String followedUid, String status);

    void remove(String followerUid, String followedUid);

    /** Solo true si existe la relación Y está ACCEPTED. */
    boolean isFollowing(String followerUid, String followedUid);

    /** True si existe la relación con status PENDING (solicitud sin responder). */
    boolean hasPendingRequest(String followerUid, String followedUid);

    /** Pasa una relación PENDING a ACCEPTED. */
    void acceptRequest(String followerUid, String followedUid);

    List<String> followersOf(String uid);
    List<String> followingOf(String uid);
    long countFollowers(String uid);
    long countFollowing(String uid);

    /** UIDs de quienes han solicitado seguirme y aún no he aceptado. */
    List<String> pendingRequestsFor(String uid);
}
