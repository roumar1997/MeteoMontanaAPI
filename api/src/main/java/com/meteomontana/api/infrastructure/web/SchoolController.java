package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.CreateNoteUseCase;
import com.meteomontana.api.application.GetNotesBySchoolUseCase;
import com.meteomontana.api.application.GetSchoolByIdUseCase;
import com.meteomontana.api.application.GetSchoolsUseCase;
import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SchoolController {

    private final GetSchoolsUseCase getSchoolsUseCase;
    private final GetSchoolByIdUseCase getSchoolByIdUseCase;
    private final GetNotesBySchoolUseCase getNotesBySchoolUseCase;
    private final GetForecastUseCase getForecastUseCase;
    private final CreateNoteUseCase createNoteUseCase;

    public SchoolController(GetSchoolsUseCase getSchoolsUseCase,
                            GetSchoolByIdUseCase getSchoolByIdUseCase,
                            GetNotesBySchoolUseCase getNotesBySchoolUseCase,
                            GetForecastUseCase getForecastUseCase,
                            CreateNoteUseCase createNoteUseCase) {
        this.getSchoolsUseCase       = getSchoolsUseCase;
        this.getSchoolByIdUseCase    = getSchoolByIdUseCase;
        this.getNotesBySchoolUseCase = getNotesBySchoolUseCase;
        this.getForecastUseCase      = getForecastUseCase;
        this.createNoteUseCase       = createNoteUseCase;
    }

    @GetMapping("/schools")
    public List<School> getSchools(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) List<String> rockType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radioKm) {
        return getSchoolsUseCase.execute(region, style, rockType, lat, lon, radioKm);
    }

    @GetMapping("/schools/{id}")
    public School getSchoolById(@PathVariable String id) {
        return getSchoolByIdUseCase.execute(id)
                .orElseThrow(() -> new SchoolNotFoundException(id));
    }

    @GetMapping("/schools/{id}/notes")
    public List<Note> getNotesBySchool(@PathVariable String id) {
        return getNotesBySchoolUseCase.execute(id);
    }

    @GetMapping("/schools/{id}/forecast")
    public ForecastResponse getForecast(@PathVariable String id) {
        return getForecastUseCase.execute(id);
    }

    @PostMapping("/schools/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Note createNote(@PathVariable String id,
                           @AuthenticationPrincipal FirebaseUser user,
                           @RequestBody CreateNoteUseCase.CreateNoteRequest req) {
        return createNoteUseCase.execute(user.uid(), id, req.text());
    }
}