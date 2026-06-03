package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUid(String uid);
    Optional<User> findByUsername(String username);
    User save(User user);
    boolean usernameTakenByOtherUser(String username, String currentUid);
    List<User> findAllWithFcmToken();
}
