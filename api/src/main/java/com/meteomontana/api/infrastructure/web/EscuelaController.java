package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.GetEscuelaByIdUseCase;
import com.meteomontana.api.application.GetEscuelasUseCase;
import com.meteomontana.api.domain.exception.EscuelaNotFoundException;
import com.meteomontana.api.domain.model.Escuela;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EscuelaController {

    private final GetEscuelasUseCase getEscuelasUseCase;
    private final GetEscuelaByIdUseCase getEscuelaByIdUseCase;

    public EscuelaController(GetEscuelasUseCase getEscuelasUseCase,
                             GetEscuelaByIdUseCase getEscuelaByIdUseCase)  {
        this.getEscuelasUseCase = getEscuelasUseCase;
        this.getEscuelaByIdUseCase = getEscuelaByIdUseCase;
    }

    @GetMapping("/escuelas")
    public List<Escuela> getEscuelas(){
        return getEscuelasUseCase.execute();
    }

    @GetMapping("/escuelas/{id}")

    public Escuela getEscuelaByIdUseCase(@PathVariable String id) {
        return getEscuelaByIdUseCase.execute(id)
                .orElseThrow(()-> new EscuelaNotFoundException(id));
    }

}
