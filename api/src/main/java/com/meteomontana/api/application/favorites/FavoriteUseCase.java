package com.meteomontana.api.application.favorites;

import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.port.FavoriteRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteUseCase {

    public record FavoriteSchoolDto(String id, String name, String region, String rockType, boolean isFavorite) {}
    public record FavoritesGridDto(List<FavoriteRow> rows, List<String> days) {}
    public record FavoriteRow(String schoolId, String schoolName, List<DayCell> cells) {}
    public record DayCell(String date, int avgScore, String label) {}

    private final FavoriteRepository favoriteRepository;
    private final SchoolRepository schoolRepository;
    private final GetForecastUseCase forecastUseCase;

    public void add(String uid, String schoolId) {
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolNotFoundException(schoolId));
        favoriteRepository.add(uid, schoolId);
    }

    public void remove(String uid, String schoolId) {
        favoriteRepository.remove(uid, schoolId);
    }

    public List<FavoriteSchoolDto> listMine(String uid) {
        List<String> ids = favoriteRepository.findSchoolIdsByUid(uid);
        List<FavoriteSchoolDto> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            schoolRepository.findById(id).ifPresent(s ->
                    result.add(new FavoriteSchoolDto(
                            s.getId(), s.getName(), s.getRegion(), s.getRockType(), true
                    )));
        }
        return result;
    }

    /**
     * Genera la tabla matriz "Favoritos · 7 días" del Tab Tiempo:
     * filas = mis favoritos, columnas = próximos 7 días con score promedio.
     */
    public FavoritesGridDto grid7Days(String uid) {
        List<String> ids = favoriteRepository.findSchoolIdsByUid(uid);
        List<FavoriteRow> rows = new ArrayList<>();
        List<String> days = new ArrayList<>();

        for (String id : ids) {
            School school = schoolRepository.findById(id).orElse(null);
            if (school == null) continue;

            ForecastResponse fc;
            try { fc = forecastUseCase.execute(id); }
            catch (Exception e) { continue; }

            List<DayCell> cells = new ArrayList<>();
            for (var d : fc.days().stream().limit(7).toList()) {
                cells.add(new DayCell(d.date(), d.avgScore(), d.scoreLabel()));
                if (days.size() < 7 && !days.contains(d.date())) days.add(d.date());
            }
            rows.add(new FavoriteRow(school.getId(), school.getName(), cells));
        }
        return new FavoritesGridDto(rows, days);
    }
}
