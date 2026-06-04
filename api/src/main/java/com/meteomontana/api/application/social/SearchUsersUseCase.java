package com.meteomontana.api.application.social;

import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.UserJpaEntity;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

@Service
public class SearchUsersUseCase {

    private final SpringDataUserRepository jpa;
    private final UserDtoMapper mapper;

    public SearchUsersUseCase(SpringDataUserRepository jpa, UserDtoMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    /** Devuelve hasta 20 usuarios públicos cuyo username/displayName contenga la query. */
    public List<PublicProfileDto> search(String query, int limit) {
        if (query == null || query.trim().isBlank()) return List.of();
        String needle = normalize(query.trim());
        // Trae todos los públicos (pequeño en BD inicial) y filtra in-memory.
        return jpa.findAll().stream()
                .filter(UserJpaEntity::isPublic)
                .filter(u -> {
                    String hay = normalize(
                            (u.getUsername() != null ? u.getUsername() : "") + " " +
                                    (u.getDisplayName() != null ? u.getDisplayName() : "")
                    );
                    return hay.contains(needle);
                })
                .limit(limit > 0 ? limit : 20)
                .map(e -> new User(
                        e.getUid(), e.getEmail(), e.getUsername(), e.getDisplayName(),
                        e.getPhotoPath(), e.getBio(), e.isPublic(), e.getTopGrade(),
                        e.isAdmin(), e.isPremium(), e.getFcmToken(),
                        e.getCreatedAt(), e.getUpdatedAt()
                ))
                .map(mapper::toPublic)
                .toList();
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase();
    }
}
