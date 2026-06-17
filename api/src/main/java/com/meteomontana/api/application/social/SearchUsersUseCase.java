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

    /** Devuelve hasta 20 usuarios cuyo username/displayName contenga la query.
     *  Los privados aparecen pero con locked=true.
     *
     *  La consulta se acota en BD (LIKE + LIMIT 100) para no cargar toda la tabla
     *  de usuarios en memoria (era un vector de DoS en un endpoint público). El
     *  segundo filtro en Java mantiene la insensibilidad a acentos sobre ese
     *  conjunto ya reducido. El cap final lo aplica el controller (máx 50). */
    public List<PublicProfileDto> search(String query, int limit) {
        if (query == null || query.trim().isBlank()) return List.of();
        String trimmed = query.trim();
        String needle = normalize(trimmed);
        return jpa.findTop100ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(trimmed, trimmed).stream()
                .filter(u -> {
                    String hay = normalize(
                            (u.getUsername() != null ? u.getUsername() : "") + " " +
                                    (u.getDisplayName() != null ? u.getDisplayName() : "")
                    );
                    return hay.contains(needle);
                })
                .limit(limit > 0 ? limit : 20)
                .map(e -> {
                    User user = new User(
                            e.getUid(), e.getEmail(), e.getUsername(), e.getDisplayName(),
                            e.getPhotoPath(), e.getBio(), e.isPublic(), e.getTopGrade(),
                            e.isAdmin(), e.isPremium(), e.getFcmToken(),
                            e.getCreatedAt(), e.getUpdatedAt()
                    );
                    return user.isPublic() ? mapper.toPublic(user) : mapper.toPublicLocked(user);
                })
                .toList();
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase();
    }
}
