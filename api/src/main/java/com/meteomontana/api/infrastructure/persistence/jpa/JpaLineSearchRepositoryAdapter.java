package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.LineSearchHit;
import com.meteomontana.api.domain.port.LineSearchRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class JpaLineSearchRepositoryAdapter implements LineSearchRepository {

    private final EntityManager em;

    public JpaLineSearchRepositoryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<LineSearchHit> searchLinesByName(String query, int limit) {
        String like = "%" + query.toLowerCase() + "%";
        List<BlockLineJpaEntity> lines = em.createQuery(
                        "SELECT l FROM BlockLineJpaEntity l WHERE lower(l.name) LIKE :q",
                        BlockLineJpaEntity.class)
                .setParameter("q", like)
                .setMaxResults(limit)
                .getResultList();
        List<LineSearchHit> out = new ArrayList<>();
        for (BlockLineJpaEntity l : lines) {
            SchoolBlockJpaEntity b = l.getBlock();
            if (b == null) continue;
            // Foto de la CARA de la vía (o la portada de la piedra): misma
            // semántica que el endpoint de bloques → la app la carga tal cual.
            String photo = l.getPhotoPath() != null ? l.getPhotoPath() : b.getPhotoPath();
            out.add(new LineSearchHit(b.getSchoolId(), null, b.getId(), b.getName(),
                    l.getId(), l.getName(), l.getGrade(), sectorNameOf(b),
                    photo, l.getLinePath(),
                    l.getStartType() != null ? l.getStartType().name() : null));
        }
        return out;
    }

    @Override
    public List<LineSearchHit> searchBlocksByName(String query, int limit) {
        String like = "%" + query.toLowerCase() + "%";
        List<SchoolBlockJpaEntity> blocks = em.createQuery(
                        "SELECT b FROM SchoolBlockJpaEntity b WHERE b.type = 'BLOCK' "
                                + "AND lower(b.name) LIKE :q", SchoolBlockJpaEntity.class)
                .setParameter("q", like)
                .setMaxResults(limit)
                .getResultList();
        List<LineSearchHit> out = new ArrayList<>();
        for (SchoolBlockJpaEntity b : blocks) {
            out.add(new LineSearchHit(b.getSchoolId(), null, b.getId(), b.getName(),
                    null, null, null, sectorNameOf(b),
                    b.getPhotoPath(), null, null));
        }
        return out;
    }

    /** Nombre del sector (ZONE) de la piedra, si lo tiene. */
    private String sectorNameOf(SchoolBlockJpaEntity b) {
        String sid = b.getSectorBlockId();
        if (sid == null || sid.isBlank()) return null;
        SchoolBlockJpaEntity sector = em.find(SchoolBlockJpaEntity.class, sid);
        return sector != null ? sector.getName() : null;
    }
}
