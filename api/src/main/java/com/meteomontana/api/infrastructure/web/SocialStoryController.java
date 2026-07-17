package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.social.SocialStoryService;
import com.meteomontana.api.application.social.SocialStoryService.ConditionsStory;
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

    public SocialStoryController(SocialStoryService service) {
        this.service = service;
    }

    /** Top escuelas de una comunidad por índice de hoy. Ej: ?region=Comunidad de Madrid */
    @GetMapping("/api/social/conditions")
    public ConditionsStory conditions(
            @RequestParam String region,
            @RequestParam(defaultValue = "5") int limit) {
        return service.conditions(region, limit);
    }

    /** Regiones con escuelas (para saber de cuáles generar historia). */
    @GetMapping("/api/social/regions")
    public List<String> regions() {
        return service.regions();
    }
}
