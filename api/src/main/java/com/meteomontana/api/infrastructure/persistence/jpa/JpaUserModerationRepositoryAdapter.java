package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.ModerationAction;
import com.meteomontana.api.domain.model.UserModerationState;
import com.meteomontana.api.domain.port.UserModerationRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserModerationRepositoryAdapter implements UserModerationRepository {

    private final SpringDataUserRepository users;
    private final SpringDataModerationActionRepository actions;

    public JpaUserModerationRepositoryAdapter(SpringDataUserRepository users,
                                              SpringDataModerationActionRepository actions) {
        this.users = users;
        this.actions = actions;
    }

    @Override
    public Optional<UserModerationState> findState(String uid) {
        return users.findById(uid).map(this::toState);
    }

    @Override
    public UserModerationState addWarning(String uid) {
        UserJpaEntity u = require(uid);
        u.setWarnings(u.getWarnings() + 1);
        return toState(users.save(u));
    }

    @Override
    public UserModerationState setSuspendedUntil(String uid, LocalDateTime until) {
        UserJpaEntity u = require(uid);
        u.setSuspendedUntil(until);
        return toState(users.save(u));
    }

    @Override
    public UserModerationState setBanned(String uid, boolean banned) {
        UserJpaEntity u = require(uid);
        u.setBanned(banned);
        if (!banned) u.setSuspendedUntil(null);   // desbanear limpia la suspensión
        return toState(users.save(u));
    }

    @Override
    public void recordAction(String adminUid, String targetUid, String action,
                             String reason, String snapshot) {
        actions.save(new ModerationActionJpaEntity(
                UUID.randomUUID().toString(), adminUid, targetUid, action, reason, snapshot));
    }

    @Override
    public List<ModerationAction> actionsOf(String targetUid) {
        return actions.findByTargetUidOrderByCreatedAtDesc(targetUid).stream()
                .map(e -> new ModerationAction(e.getId(), e.getAdminUid(), e.getTargetUid(),
                        e.getAction(), e.getReason(), e.getSnapshot(), e.getCreatedAt()))
                .toList();
    }

    private UserJpaEntity require(String uid) {
        return users.findById(uid)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + uid));
    }

    private UserModerationState toState(UserJpaEntity u) {
        return new UserModerationState(u.getUid(), u.getUsername(), u.getDisplayName(),
                u.isBanned(), u.getSuspendedUntil(), u.getWarnings());
    }
}
