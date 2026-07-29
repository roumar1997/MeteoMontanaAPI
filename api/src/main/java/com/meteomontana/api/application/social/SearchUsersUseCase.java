package com.meteomontana.api.application.social;

import com.meteomontana.api.application.users.PublicProfileDto;
import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchUsersUseCase {

    private final UserRepository users;
    private final UserDtoMapper mapper;

    /** Devuelve hasta 20 usuarios cuyo username/displayName contenga la query.
     *  Los privados aparecen pero con locked=true.
     *
     *  La consulta se acota en BD (LIKE + LIMIT, ver el puerto) para no cargar
     *  toda la tabla de usuarios en memoria (era un vector de DoS en un endpoint
     *  público). El segundo filtro en Java mantiene la insensibilidad a acentos
     *  sobre ese conjunto ya reducido. El cap final lo aplica el controller. */
    public List<PublicProfileDto> search(String query, int limit) {
        if (query == null || query.trim().isBlank()) return List.of();
        String trimmed = query.trim();
        String needle = normalize(trimmed);
        return users.searchByUsernameOrDisplayName(trimmed).stream()
                .filter(u -> {
                    String hay = normalize(
                            (u.getUsername() != null ? u.getUsername() : "") + " " +
                                    (u.getDisplayName() != null ? u.getDisplayName() : "")
                    );
                    return hay.contains(needle);
                })
                .limit(limit > 0 ? limit : 20)
                .map(u -> u.isPublic() ? mapper.toPublic(u) : mapper.toPublicLocked(u))
                .toList();
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase();
    }
}
