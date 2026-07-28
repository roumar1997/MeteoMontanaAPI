package com.meteomontana.api.application.community;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.exception.ForbiddenException;
import com.meteomontana.api.domain.exception.NotFoundException;
import com.meteomontana.api.domain.model.CommunityVotes.GradeSummary;
import com.meteomontana.api.domain.model.CommunityVotes.GradeVote;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationSummary;
import com.meteomontana.api.domain.model.CommunityVotes.OrientationVote;
import com.meteomontana.api.domain.model.CommunityVotes.SunHour;
import com.meteomontana.api.domain.model.CommunityVotes.SunHours;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.School;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.CommunityVoteRepository;
import com.meteomontana.api.domain.port.JournalRepository;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.score.GradeConsensus;
import com.meteomontana.api.domain.score.SunPositionCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Votación comunitaria SIN admin (decisiones de Rodrigo 2026-07-29):
 *  - Orientación: cualquiera vota (se ve desde el suelo); gana la mayoría.
 *    photoIndex NULL = piedra/sector entero; en muros, por foto.
 *  - Grado: solo vota quien tiene la vía en su DIARIO (autoridad del que la
 *    ha probado). Con 3+ votos el consenso se muestra en topos y buscador Y
 *    se propaga a los diarios (perfiles) — el grado del equipador queda como
 *    referencia en setter_grade.
 */
@Service
public class CommunityVoteUseCase {

    private static final Set<String> ASPECTS = Set.of("N", "NE", "E", "SE", "S", "SO", "O", "NO");
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final CommunityVoteRepository votes;
    private final SchoolBlockRepository blocks;
    private final SchoolRepository schools;
    private final JournalRepository journal;

    public CommunityVoteUseCase(CommunityVoteRepository votes, SchoolBlockRepository blocks,
                                SchoolRepository schools, JournalRepository journal) {
        this.votes = votes;
        this.blocks = blocks;
        this.schools = schools;
        this.journal = journal;
    }

    // ── Orientación ─────────────────────────────────────────────────────────

    /** Resúmenes por superficie: índice de foto (o null = entero) → recuento/consenso/mi voto. */
    public List<OrientationSummary> orientationSummaries(String blockId, String uid) {
        List<OrientationVote> all = votes.findOrientationVotes(blockId);
        Map<Integer, List<OrientationVote>> byPhoto = all.stream()
                .collect(Collectors.groupingBy(
                        v -> v.photoIndex() == null ? -1 : v.photoIndex()));
        return byPhoto.entrySet().stream()
                .map(e -> summarize(e.getKey() == -1 ? null : e.getKey(), e.getValue(), uid))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<OrientationSummary> voteOrientation(String blockId, Integer photoIndex,
                                                    String aspect, String uid) {
        if (aspect == null || !ASPECTS.contains(aspect.trim().toUpperCase()))
            throw new BadRequestException("Rumbo inválido: usa N, NE, E, SE, S, SO, O, NO");
        blocks.findById(blockId).orElseThrow(() -> new NotFoundException("Piedra no encontrada"));
        votes.upsertOrientationVote(new OrientationVote(
                blockId, photoIndex, uid, aspect.trim().toUpperCase()));
        return orientationSummaries(blockId, uid);
    }

    private OrientationSummary summarize(Integer photoIndex, List<OrientationVote> list, String uid) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String mine = null;
        for (OrientationVote v : list) {
            counts.merge(v.aspect(), 1, Integer::sum);
            if (v.voterUid().equals(uid)) mine = v.aspect();
        }
        return new OrientationSummary(photoIndex, counts,
                GradeConsensus.orientationConsensus(counts), mine);
    }

    // ── Sol/sombra ──────────────────────────────────────────────────────────

    /**
     * Tira horaria de sol de HOY (8:00-21:00) para la superficie: usa el
     * consenso votado; sin votos ni orientación → aspecto null y tira vacía.
     */
    public SunHours sunHours(String blockId, Integer photoIndex, LocalDate day) {
        SchoolBlock block = blocks.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Piedra no encontrada"));
        String aspect = orientationSummaries(blockId, "").stream()
                .filter(s -> Objects.equals(s.photoIndex(), photoIndex))
                .map(OrientationSummary::consensus)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        if (aspect == null) return new SunHours(null, List.of());

        double lat = block.getLat(), lon = block.getLon();
        int offsetSeconds = MADRID.getRules()
                .getOffset(day.atStartOfDay(MADRID).toInstant()).getTotalSeconds();
        List<SunHour> hours = new java.util.ArrayList<>();
        for (int h = 8; h <= 21; h++) {
            LocalDateTime t = day.atTime(h, 0);
            hours.add(new SunHour(t.toString(),
                    SunPositionCalculator.isWallInSun(aspect, t, lat, lon, offsetSeconds)));
        }
        return new SunHours(aspect, hours);
    }

    // ── Grado ───────────────────────────────────────────────────────────────

    public GradeSummary gradeSummary(String lineId, String uid) {
        BlockLine line = findLine(lineId);
        Map<String, Integer> counts = new HashMap<>();
        String mine = null;
        for (GradeVote v : votes.findGradeVotes(lineId)) {
            counts.merge(v.grade(), 1, Integer::sum);
            if (v.voterUid().equals(uid)) mine = v.grade();
        }
        String setter = line.getSetterGrade() != null ? line.getSetterGrade() : line.getGrade();
        return new GradeSummary(lineId, counts, setter,
                GradeConsensus.displayedGrade(counts, setter, line.getGrade()), mine);
    }

    @Transactional
    public GradeSummary voteGrade(String lineId, String grade, String uid) {
        if (grade == null || grade.isBlank() || grade.length() > 8)
            throw new BadRequestException("Grado inválido");
        findLine(lineId);
        // Solo vota quien la tiene en el diario (probada o encadenada).
        boolean inJournal = journal.findByUid(uid).stream()
                .anyMatch(s -> lineId.equals(s.getLineId()));
        if (!inJournal)
            throw new ForbiddenException("GRADE_VOTE_REQUIRES_JOURNAL");

        votes.upsertGradeVote(new GradeVote(lineId, uid, grade.trim()));
        GradeSummary summary = gradeSummary(lineId, uid);
        // Propagar el mostrado (vía + diarios) si cambió.
        BlockLine line = findLine(lineId);
        if (!Objects.equals(summary.displayedGrade(), line.getGrade())) {
            votes.applyDisplayedGrade(lineId, summary.displayedGrade());
        }
        return summary;
    }

    private BlockLine findLine(String lineId) {
        SchoolBlock block = blocks.findByLineId(lineId)
                .orElseThrow(() -> new NotFoundException("Vía no encontrada"));
        return block.getLines().stream()
                .filter(l -> lineId.equals(l.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Vía no encontrada"));
    }

    /** El nombre de la escuela solo para textos de la UI (opcional). */
    public String schoolName(String schoolId) {
        return schools.findById(schoolId).map(School::getName).orElse(schoolId);
    }
}
