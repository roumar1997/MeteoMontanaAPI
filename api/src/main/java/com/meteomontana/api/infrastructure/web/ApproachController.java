package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.approach.GetApproachesUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público: aproximaciones (caminos) de una escuela con sus chinchetas. Fase 1
 * de APPROACH_DESIGN.md — solo lectura, mismo nivel de acceso que el catálogo
 * de escuelas/bloques.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApproachController {

    private final GetApproachesUseCase useCase;

    @GetMapping("/schools/{id}/approaches")
    public List<GetApproachesUseCase.ApproachDto> list(@PathVariable String id) {
        return useCase.listBySchool(id);
    }
}
