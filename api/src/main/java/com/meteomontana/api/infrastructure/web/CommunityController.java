package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.community.GetTopContributorsUseCase;
import com.meteomontana.api.application.community.GetTopContributorsUseCase.TopContributorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de comunidad. Público: el ranking solo expone datos que ya son
 * públicos (username, displayName, foto y nº de aportaciones aprobadas).
 */
@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final GetTopContributorsUseCase topContributors;

    /** Ranking de mayores contribuidores (contribuciones APROBADAS).
     *  Si vienen year+month, es el ranking de ESE mes (pasado o en curso) en
     *  vez del total histórico — pestaña "por meses" del trofeo del feed. */
    @GetMapping("/api/community/top-contributors")
    public List<TopContributorDto> topContributors(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year != null && month != null) {
            return topContributors.topContributorsForMonth(year, month, limit);
        }
        return topContributors.topContributors(limit);
    }
}
