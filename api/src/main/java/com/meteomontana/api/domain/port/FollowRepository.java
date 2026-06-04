package com.meteomontana.api.domain.port;

import java.util.List;

public interface FollowRepository {
    void add(String followerUid, String followedUid);
    void remove(String followerUid, String followedUid);
    boolean isFollowing(String followerUid, String followedUid);
    List<String> followersOf(String uid);
    List<String> followingOf(String uid);
    long countFollowers(String uid);
    long countFollowing(String uid);
}
