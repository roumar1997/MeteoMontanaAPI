package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.community.GetTopContributorsUseCase;
import com.meteomontana.api.application.community.GetTopContributorsUseCase.TopContributorDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de comunidad. Público: el ranking solo expone datos que ya son
 * públicos (username, displayName, foto y nº de aportaciones aprobadas).
 */
@RestController
public class CommunityController {

    private final GetTopContributorsUseCase topContributors;

    public CommunityController(GetTopContributorsUseCase topContributors) {
        this.topContributors = topContributors;
    }

    /** Ranking de mayores contribuidores (contribuciones APROBADAS). */
    @GetMapping("/api/community/top-contributors")
    public List<TopContributorDto> topContributors(
            @RequestParam(defaultValue = "20") int limit) {
        return topContributors.topContributors(limit);
    }
}
