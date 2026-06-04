package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.blocks.SchoolBlockUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SchoolBlockController {

    private final SchoolBlockUseCase useCase;

    public SchoolBlockController(SchoolBlockUseCase useCase) {
        this.useCase = useCase;
    }

    /** Público: listar bloques de la escuela. */
    @GetMapping("/schools/{id}/blocks")
    public List<SchoolBlockUseCase.BlockDto> list(@PathVariable String id) {
        return useCase.listBySchool(id);
    }

    /** Público: detalle de un bloque (con sus líneas). */
    @GetMapping("/blocks/{id}")
    public SchoolBlockUseCase.BlockDto get(@PathVariable String id) {
        return useCase.findById(id);
    }

    /** Auth: crear bloque (cualquier user; el admin lo creará directamente,
     *  usuarios mandarán propuestas vía submissions en el futuro). */
    @PostMapping("/schools/{id}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolBlockUseCase.BlockDto create(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String id,
            @RequestBody SchoolBlockUseCase.CreateBlockRequest req) {
        return useCase.create(user.uid(), id, req);
    }

    @PutMapping("/blocks/{blockId}")
    public SchoolBlockUseCase.BlockDto update(
            @AuthenticationPrincipal FirebaseUser user,
            @PathVariable String blockId,
            @RequestBody SchoolBlockUseCase.CreateBlockRequest req) {
        return useCase.update(user.uid(), blockId, req);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal FirebaseUser user,
                       @PathVariable String blockId) {
        useCase.delete(user.uid(), blockId);
    }
}
