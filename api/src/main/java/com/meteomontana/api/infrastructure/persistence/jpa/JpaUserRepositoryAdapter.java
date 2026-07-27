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
    public List<User> findByUids(java.util.Collection<String> uids) {
        // uid es la @Id de UserJpaEntity → findAllById resuelve en UNA query.
        return jpaRepo.findAllById(uids).stream().map(this::toDomain).toList();
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

    @Override
    public List<User> searchByUsernameOrDisplayName(String query) {
        // Acotado en BD (LIKE + top 100) — no cargar toda la tabla en memoria.
        return jpaRepo.findTop100ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count() { return jpaRepo.count(); }

    @Override
    public long countAdmins() { return jpaRepo.countAdmins(); }

    @Override
    public List<User> findAdmins() {
        return jpaRepo.findByIsAdminTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public List<User> findRecent(int limit) {
        return jpaRepo.findAll(org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream().map(this::toDomain).toList();
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
