package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Búsqueda GLOBAL de vías y bloques por nombre (buscador de la pantalla de
 * Escuelas). Pública, como el catálogo. Devuelve como mucho 20 resultados:
 * vías primero, piedras después, cada uno con su escuela para navegar.
 */
@RestController
@RequestMapping("/api/search")
public class LineSearchController {

    public record LineHit(String schoolId, String schoolName,
                          String blockId, String blockName,
                          String lineId, String lineName, String grade,
                          String sectorName) {}

    private final EntityManager em;
    private final SchoolRepository schools;

    public LineSearchController(EntityManager em, SchoolRepository schools,
                                SpringDataSchoolBlockRepository unusedButKeepsWiring) {
        this.em = em;
        this.schools = schools;
    }

    @GetMapping("/lines")
    public List<LineHit> search(@RequestParam String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return List.of();
        String like = "%" + query.toLowerCase() + "%";

        List<LineHit> out = new ArrayList<>();
        Map<String, String> schoolNames = new HashMap<>();

        // Vías cuyo nombre casa.
        List<BlockLineJpaEntity> lines = em.createQuery(
                "SELECT l FROM BlockLineJpaEntity l WHERE lower(l.name) LIKE :q",
                BlockLineJpaEntity.class)
                .setParameter("q", like)
                .setMaxResults(15)
                .getResultList();
        for (BlockLineJpaEntity l : lines) {
            SchoolBlockJpaEntity b = l.getBlock();
            if (b == null) continue;
            String sName = schoolNames.computeIfAbsent(b.getSchoolId(),
                    id -> schools.findById(id).map(s -> s.getName()).orElse(""));
            out.add(new LineHit(b.getSchoolId(), sName, b.getId(), b.getName(),
                    l.getId(), l.getName(), l.getGrade(), sectorNameOf(b)));
        }

        // Piedras/muros cuyo nombre casa (solo BLOCK con nombre no numérico:
        // "1", "2"… son autonumeradas y ensuciarían la búsqueda).
        List<SchoolBlockJpaEntity> blocks = em.createQuery(
                "SELECT b FROM SchoolBlockJpaEntity b WHERE b.type = 'BLOCK' "
                        + "AND lower(b.name) LIKE :q", SchoolBlockJpaEntity.class)
                .setParameter("q", like)
                .setMaxResults(10)
                .getResultList();
        for (SchoolBlockJpaEntity b : blocks) {
            if (b.getName() != null && b.getName().matches("\\d+")) continue;
            String sName = schoolNames.computeIfAbsent(b.getSchoolId(),
                    id -> schools.findById(id).map(s -> s.getName()).orElse(""));
            out.add(new LineHit(b.getSchoolId(), sName, b.getId(), b.getName(),
                    null, null, null, sectorNameOf(b)));
        }
        return out.size() > 20 ? out.subList(0, 20) : out;
    }

    /** Nombre del sector (ZONE) de la piedra, si lo tiene. */
    private String sectorNameOf(SchoolBlockJpaEntity b) {
        String sid = b.getSectorBlockId();
        if (sid == null || sid.isBlank()) return null;
        SchoolBlockJpaEntity sector = em.find(SchoolBlockJpaEntity.class, sid);
        return sector != null ? sector.getName() : null;
    }
}
