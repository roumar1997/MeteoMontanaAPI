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
        return toDtos(statsRepo.topContributors(SubmissionStatus.APPROVED, capped));
    }

    /** Ranking de los últimos {@code days} días (por fecha de aprobación).
     *  Para la historia semanal "aportadores de la semana". */
    public List<TopContributorDto> topContributorsSince(int days, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_LIMIT));
        var since = java.time.LocalDateTime.now().minusDays(Math.max(1, days));
        return toDtos(statsRepo.topContributorsSince(SubmissionStatus.APPROVED, since, capped));
    }

    /** Mapea counts → DTOs con perfil (respeta privacidad), una query de perfiles. */
    private List<TopContributorDto> toDtos(
            List<com.meteomontana.api.domain.port.ContributionStatsRepository.ContributorCount> counts) {
        if (counts.isEmpty()) return List.of();

        Map<String, User> users = userRepository
                .findByUids(counts.stream().map(c -> c.uid()).toList())
                .stream()
                .collect(Collectors.toMap(User::getUid, Function.identity()));

        return counts.stream()
                .map(c -> {
                    User u = users.get(c.uid());
                    if (u == null) return null; // cuenta borrada → fuera del ranking
                    var profile = u.isPublic() ? mapper.toPublic(u) : mapper.toPublicLocked(u);
                    return new TopContributorDto(
                            u.getUid(), profile.username(), profile.displayName(),
                            profile.photoUrl(), c.count());
                })
                .filter(row -> row != null)
                .toList();
    }
}
