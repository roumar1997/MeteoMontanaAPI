package com.meteomontana.api.application.community;

import com.meteomontana.api.application.users.UserDtoMapper;
import com.meteomontana.api.domain.model.SubmissionStatus;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.ContributionStatsRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ranking de mayores contribuidores: usuarios con más contribuciones APROBADAS
 * (parkings, piedras, sectores, correcciones... ya materializadas en el mapa).
 *
 * Dos queries en total: el GROUP BY del ranking + un findByUids para los
 * perfiles (nada de N+1). Respeta la privacidad igual que la búsqueda de
 * usuarios: perfil privado → vista "locked" (sin bio/grado, con username/foto).
 */
@Service
public class GetTopContributorsUseCase {

    /** Fila del ranking que consume la app. */
    public record TopContributorDto(
            String uid,
            String username,
            String displayName,
            String photoUrl,
            long approvedCount
    ) {}

    private static final int MAX_LIMIT = 50;

    private final ContributionStatsRepository statsRepo;
    private final UserRepository userRepository;
    private final UserDtoMapper mapper;

    public GetTopContributorsUseCase(ContributionStatsRepository statsRepo,
                                     UserRepository userRepository,
                                     UserDtoMapper mapper) {
        this.statsRepo = statsRepo;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public List<TopContributorDto> topContributors(int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_LIMIT));
        var counts = statsRepo.topContributors(SubmissionStatus.APPROVED, capped);
        if (counts.isEmpty()) return List.of();

        Map<String, User> users = userRepository
                .findByUids(counts.stream().map(c -> c.uid()).toList())
                .stream()
                .collect(Collectors.toMap(User::getUid, Function.identity()));

        return counts.stream()
                .map(c -> {
                    User u = users.get(c.uid());
                    if (u == null) return null; // cuenta borrada → fuera del ranking
                    // Misma regla de privacidad que la búsqueda de usuarios.
                    var profile = u.isPublic() ? mapper.toPublic(u) : mapper.toPublicLocked(u);
                    return new TopContributorDto(
                            u.getUid(), profile.username(), profile.displayName(),
                            profile.photoUrl(), c.count());
                })
                .filter(row -> row != null)
                .toList();
    }
}
