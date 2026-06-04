package com.meteomontana.api.domain.port;

import java.util.List;

public interface FavoriteRepository {
    void add(String uid, String schoolId);
    void remove(String uid, String schoolId);
    boolean exists(String uid, String schoolId);
    List<String> findSchoolIdsByUid(String uid);
}
