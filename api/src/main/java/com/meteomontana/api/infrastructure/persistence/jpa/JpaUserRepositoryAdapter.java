package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepo;

    public JpaUserRepositoryAdapter(SpringDataUserRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Optional<User> findByUid(String uid) {
        return jpaRepo.findById(uid).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepo.findByUsernameIgnoreCase(username).map(this::toDomain);
    }

    @Override
    public User save(User u) {
        UserJpaEntity entity = new UserJpaEntity(
                u.getUid(), u.getEmail(), u.getUsername(), u.getDisplayName(),
                u.getPhotoPath(), u.getBio(), u.isPublic(), u.getTopGrade(),
                u.isAdmin(), u.isPremium(), u.getFcmToken(), u.getGender(),
                u.getGearJson(), u.getCreatedAt(), u.getUpdatedAt()
        );
        return toDomain(jpaRepo.save(entity));
    }

    @Override
    public boolean usernameTakenByOtherUser(String username, String currentUid) {
        return jpaRepo.findByUsernameIgnoreCase(username)
                .map(e -> !e.getUid().equals(currentUid))
                .orElse(false);
    }

    @Override
    public List<User> findAllWithFcmToken() {
        return jpaRepo.findAllByFcmTokenIsNotNull().stream().map(this::toDomain).toList();
    }

    private User toDomain(UserJpaEntity e) {
        return new User(
                e.getUid(), e.getEmail(), e.getUsername(), e.getDisplayName(),
                e.getPhotoPath(), e.getBio(), e.isPublic(), e.getTopGrade(),
                e.isAdmin(), e.isPremium(), e.getFcmToken(), e.getGender(),
                e.getGearJson(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
