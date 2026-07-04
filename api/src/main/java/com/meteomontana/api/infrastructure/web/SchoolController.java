package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.CreateNoteUseCase;
import com.meteomontana.api.application.GetNotesBySchoolUseCase;
import com.meteomontana.api.application.note.NoteVotesService;
import com.meteomontana.api.application.GetSchoolByIdUseCase;
import com.meteomontana.api.application.GetSchoolsUseCase;
import com.meteomontana.api.application.SearchSchoolsUseCase;
import com.meteomontana.api.application.forecast.ForecastResponse;
import com.meteomontana.api.application.forecast.GetForecastUseCase;
import com.meteomontana.api.application.forecast.GetMonthlyStatsUseCase;
import com.meteomontana.api.domain.exception.SchoolNotFoundException;
import com.meteomontana.api.domain.model.Note;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SchoolController {

    private final GetSchoolsUseCase getSchoolsUseCase;
    private final GetSchoolByIdUseCase getSchoolByIdUseCase;
    private final GetNotesBySchoolUseCase getNotesBySchoolUseCase;
    private final GetForecastUseCase getForecastUseCase;
    private final CreateNoteUseCase createNoteUseCase;
    private final NoteVotesService noteVotesService;
    private final com.meteomontana.api.application.moderation.ContentModerationService moderationService;
    private final SearchSchoolsUseCase searchSchoolsUseCase;
    private final GetMonthlyStatsUseCase getMonthlyStatsUseCase;
    private final ObjectMapper objectMapper;

    public SchoolController(GetSchoolsUseCase getSchoolsUseCase,
                            GetSchoolByIdUseCase getSchoolByIdUseCase,
                            GetNotesBySchoolUseCase getNotesBySchoolUseCase,
                            GetForecastUseCase getForecastUseCase,
                            CreateNoteUseCase createNoteUseCase,
                            NoteVotesService noteVotesService,
                            com.meteomontana.api.application.moderation.ContentModerationService moderationService,
                            SearchSchoolsUseCase searchSchoolsUseCase,
                            GetMonthlyStatsUseCase getMonthlyStatsUseCase,
                            ObjectMapper objectMapper) {
        this.getSchoolsUseCase       = getSchoolsUseCase;
        this.getSchoolByIdUseCase    = getSchoolByIdUseCase;
        this.getNotesBySchoolUseCase = getNotesBySchoolUseCase;
        this.getForecastUseCase      = getForecastUseCase;
        this.createNoteUseCase       = createNoteUseCase;
        this.noteVotesService        = noteVotesService;
        this.moderationService       = moderationService;
        this.searchSchoolsUseCase    = searchSchoolsUseCase;
        this.getMonthlyStatsUseCase  = getMonthlyStatsUseCase;
        this.objectMapper            = objectMapper;
    }

    @GetMapping("/schools/search")
    public List<School> search(@RequestParam("q") String query,
                                @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return searchSchoolsUseCase.execute(query, limit);
    }

    /**
     * Catálogo con soporte ETag/304: calculamos un hash del contenido y, si el
     * cliente manda If-None-Match con el mismo valor, respondemos 304 sin body.
     * El cliente (Ktor en Android) reusa entonces su caché local y nos ahorramos
     * serializar + transferir las ~191 escuelas en cada apertura de la app.
     */
    @GetMapping("/schools")
    public ResponseEntity<List<School>> getSchools(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) List<String> rockType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) Double radioKm,
            WebRequest request) {
        List<School> schools = getSchoolsUseCase.execute(region, style, rockType, lat, lon, radioKm);
        String etag = etagOf(schools);
        // checkNotModified compara con If-None-Match y, si coincide, deja la
        // respuesta preparada como 304 (incluye el header ETag automáticamente).
        if (request.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().eTag(etag).body(schools);
    }

    /** Hash SHA-256 del JSON del catálogo. Cambia si cambia cualquier escuela. */
    private String etagOf(List<School> schools) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(schools);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json);
            // 16 bytes bastan para no colisionar y dejan un header corto.
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular el ETag del catálogo", e);
        }
    }

    @GetMapping("/schools/{id}")
    public School getSchoolById(@PathVariable String id) {
        return getSchoolByIdUseCase.execute(id)
                .orElseThrow(() -> new SchoolNotFoundException(id));
    }

    @GetMapping("/schools/{id}/notes")
    public List<NoteVotesService.NoteWithMyVote> getNotesBySchool(
            @PathVariable String id,
            @AuthenticationPrincipal FirebaseUser user) {
        // Ordenadas por utilidad (me gusta − no me gusta) y con el voto del
        // usuario que consulta (0 si es anónimo).
        var notes = getNotesBySchoolUseCase.execute(id);
        if (user != null) {
            var blocked = moderationService.blockedBy(user.uid());
            if (!blocked.isEmpty()) {
                notes = notes.stream().filter(n -> !blocked.contains(n.getUid())).toList();
            }
        }
        return noteVotesService.enrichAndSort(notes, user != null ? user.uid() : null);
    }

    @GetMapping("/schools/{id}/forecast")
    public ForecastResponse getForecast(@PathVariable String id) {
        return getForecastUseCase.execute(id);
    }

    /** Scores mensuales históricos (12 valores 0-100) + mejor rango de meses. */
    @GetMapping("/schools/{id}/monthly-stats")
    public GetMonthlyStatsUseCase.MonthlyStatsResponse getMonthlyStats(@PathVariable String id) {
        return getMonthlyStatsUseCase.execute(id);
    }

    /** Voto de utilidad: {"value":1|-1}. Repetir el voto lo retira. */
    @PostMapping("/notes/{noteId}/vote")
    public java.util.Map<String, Integer> voteNote(@PathVariable String noteId,
                                                   @AuthenticationPrincipal FirebaseUser user,
                                                   @RequestBody java.util.Map<String, Integer> body) {
        int myVote = noteVotesService.vote(user.uid(), noteId,
                body.getOrDefault("value", 0));
        return java.util.Map.of("myVote", myVote);
    }

    @PostMapping("/schools/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public Note createNote(@PathVariable String id,
                           @AuthenticationPrincipal FirebaseUser user,
                           @RequestBody CreateNoteUseCase.CreateNoteRequest req) {
        return createNoteUseCase.execute(user.uid(), id, req.text(), req.photoUrl());
    }
}