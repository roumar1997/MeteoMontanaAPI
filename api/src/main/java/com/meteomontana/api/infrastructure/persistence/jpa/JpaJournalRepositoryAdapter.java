package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.JournalSession;
import com.meteomontana.api.domain.port.JournalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaJournalRepositoryAdapter implements JournalRepository {

    private final SpringDataJournalRepository jpaRepo;

    public JpaJournalRepositoryAdapter(SpringDataJournalRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public JournalSession save(JournalSession s) {
        JournalSessionJpaEntity e = new JournalSessionJpaEntity(
                s.getId(), s.getUid(), s.getSchoolId(), s.getSchoolName(),
                s.getSector(), s.getBlockName(), s.getGrade(), s.getNotes(),
                s.getSessionDate(), s.getCreatedAt()
        );
        e.setDiscipline(s.getDiscipline());
        e.setLineId(s.getLineId());
        e.setStatus(s.getStatus());
        e.setAVista(s.isAVista());
        e.setAlFlash(s.isAlFlash());
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public Optional<JournalSession> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<JournalSession> findByUid(String uid) {
        return jpaRepo.findByUidOrderBySessionDateDesc(uid).stream().map(this::toDomain).toList();
    }

    @Override
    public void updateSessionDate(String id, java.time.LocalDate newDate) {
        jpaRepo.findById(id).ifPresent(e -> {
            e.setSessionDate(newDate);
            jpaRepo.save(e);
        });
    }

    @Override
    public void updateStyle(String id, boolean aVista, boolean alFlash) {
        jpaRepo.findById(id).ifPresent(e -> {
            e.setAVista(aVista);
            e.setAlFlash(alFlash);
            jpaRepo.save(e);
        });
    }

    @Override
    public void deleteById(String id) {
        jpaRepo.deleteById(id);
    }

    private JournalSession toDomain(JournalSessionJpaEntity e) {
        return new JournalSession(
                e.getId(), e.getUid(), e.getSchoolId(), e.getSchoolName(),
                e.getSector(), e.getBlockName(), e.getGrade(), e.getNotes(),
                e.getDiscipline(), e.getLineId(), e.getStatus(),
                e.isAVista(), e.isAlFlash(), e.getSessionDate(), e.getCreatedAt()
        );
    }

    @Override
    public int updateNameByLineId(String lineId, String name) {
        return jpaRepo.updateNameByLineId(lineId, name);
    }
}
