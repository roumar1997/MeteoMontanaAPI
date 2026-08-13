package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.approach.AddApproachPinUseCase;
import com.meteomontana.api.application.approach.CreateApproachUseCase;
import com.meteomontana.api.application.approach.DeleteApproachPinUseCase;
import com.meteomontana.api.application.approach.DeleteApproachUseCase;
import com.meteomontana.api.application.approach.GetApproachesUseCase;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aproximaciones (caminos) de una escuela con sus chinchetas.
 *
 * Lectura: pública, mismo nivel de acceso que el catálogo de escuelas/bloques.
 * Escritura: SOLO ADMIN por ahora — grabar/añadir chincheta para cualquier
 * usuario espera a la consulta legal (APPROACH_DESIGN.md §2.6/§10).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApproachController {

    private final GetApproachesUseCase useCase;
    private final CreateApproachUseCase createUseCase;
    private final AddApproachPinUseCase addPinUseCase;
    private final DeleteApproachUseCase deleteUseCase;
    private final DeleteApproachPinUseCase deletePinUseCase;

    @GetMapping("/schools/{id}/approaches")
    public List<GetApproachesUseCase.ApproachDto> list(@PathVariable String id) {
        return useCase.listBySchool(id);
    }

    @PostMapping("/schools/{id}/approaches")
    public GetApproachesUseCase.ApproachDto create(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody CreateApproachUseCase.CreateApproachRequest req) {
        return createUseCase.create(user.uid(), id, req);
    }

    @PostMapping("/approaches/{id}/pins")
    public GetApproachesUseCase.ApproachPinDto addPin(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUser user,
            @RequestBody AddApproachPinUseCase.AddPinRequest req) {
        return addPinUseCase.add(user.uid(), id, req);
    }

    @DeleteMapping("/approaches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, @AuthenticationPrincipal FirebaseUser user) {
        deleteUseCase.delete(user.uid(), id);
    }

    @DeleteMapping("/approaches/pins/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePin(@PathVariable String id, @AuthenticationPrincipal FirebaseUser user) {
        deletePinUseCase.delete(user.uid(), id);
    }
}
