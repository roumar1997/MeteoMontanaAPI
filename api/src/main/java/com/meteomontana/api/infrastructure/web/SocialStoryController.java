package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.community.GetTopContributorsUseCase;
import com.meteomontana.api.application.community.GetTopContributorsUseCase.TopContributorDto;
import com.meteomontana.api.application.social.SocialStoryHtml;
import com.meteomontana.api.application.social.SocialStoryService;
import com.meteomontana.api.application.social.SocialStoryService.ConditionsStory;
import com.meteomontana.api.application.social.SocialStoryService.NoveltyStory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Datos de las historias sociales automáticas (proyecto n8n / Instagram).
 * Público: solo datos ya públicos (catálogo + forecast). El render a imagen
 * lo hace n8n sobre el HTML (endpoints .html se añadirán después).
 */
@RestController
public class SocialStoryController {

    private final SocialStoryService service;
    private final GetTopContributorsUseCase contributors;

    public SocialStoryController(SocialStoryService service,
                                 GetTopContributorsUseCase contributors) {
        this.service = service;
        this.contributors = contributors;
    }

    /** Top escuelas de una comunidad por índice de hoy. Ej: ?region=Comunidad de Madrid */
    @GetMapping("/api/social/conditions")
    public ConditionsStory conditions(
            @RequestParam String region,
            @RequestParam(defaultValue = "5") int limit) {
        return service.conditions(region, limit);
    }

    /** Novedades de los últimos {@code days} días (piedras/vías nuevas por escuela). */
    @GetMapping("/api/social/novelties")
    public NoveltyStory novelties(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "6") int limit) {
        return service.novelties(days, limit);
    }

    /** Aportadores de los últimos {@code days} días (ranking semanal). */
    @GetMapping("/api/social/contributors")
    public List<TopContributorDto> contributors(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int limit) {
        return contributors.topContributorsSince(days, limit);
    }

    /** HTML 1080x1920 de la historia de condiciones (n8n lo captura a PNG).
     *  Ruta con barra (no .html) para que Spring no la trate como recurso estático. */
    @GetMapping(value = "/api/social/conditions/html", produces = MediaType.TEXT_HTML_VALUE)
    public String conditionsHtml(
            @RequestParam String region,
            @RequestParam(defaultValue = "5") int limit) {
        return SocialStoryHtml.conditions(service.conditions(region, limit));
    }

    /** HTML de la historia de novedades de la semana. */
    @GetMapping(value = "/api/social/novelties/html", produces = MediaType.TEXT_HTML_VALUE)
    public String noveltiesHtml(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "6") int limit) {
        return SocialStoryHtml.novelties(service.novelties(days, limit));
    }

    /** HTML del ranking de aportadores. days=0 → global (mensual); days>0 → semanal. */
    @GetMapping(value = "/api/social/contributors/html", produces = MediaType.TEXT_HTML_VALUE)
    public String contributorsHtml(
            @RequestParam(defaultValue = "0") int days,
            @RequestParam(defaultValue = "5") int limit) {
        var people = days > 0
                ? contributors.topContributorsSince(days, limit)
                : contributors.topContributors(limit);
        return SocialStoryHtml.contributors(people, days > 0);
    }

    /** Regiones con escuelas (para saber de cuáles generar historia). */
    @GetMapping("/api/social/regions")
    public List<String> regions() {
        return service.regions();
    }
}
