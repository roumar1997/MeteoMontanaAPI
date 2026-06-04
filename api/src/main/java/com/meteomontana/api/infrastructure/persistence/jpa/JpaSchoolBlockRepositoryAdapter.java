package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSchoolBlockRepositoryAdapter implements SchoolBlockRepository {

    private final SpringDataSchoolBlockRepository jpaRepo;

    public JpaSchoolBlockRepositoryAdapter(SpringDataSchoolBlockRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public SchoolBlock save(SchoolBlock b) {
        SchoolBlockJpaEntity e = new SchoolBlockJpaEntity(
                b.getId(), b.getSchoolId(), b.getType(), b.getName(),
                b.getLat(), b.getLon(), b.getPhotoPath(), b.getDescription(),
                b.getCreatedByUid(), b.getCreatedAt()
        );
        // Líneas
        b.getLines().forEach(line -> {
            e.addLine(new BlockLineJpaEntity(
                    line.getId(), line.getName(), line.getGrade(),
                    line.getStartType(), line.getLinePath(), line.getSortOrder()
            ));
        });
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public Optional<SchoolBlock> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<SchoolBlock> findBySchoolId(String schoolId) {
        return jpaRepo.findBySchoolIdOrderByCreatedAtAsc(schoolId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String id) { jpaRepo.deleteById(id); }

    private SchoolBlock toDomain(SchoolBlockJpaEntity e) {
        List<BlockLine> lines = e.getLines().stream().map(l -> new BlockLine(
                l.getId(), e.getId(), l.getName(), l.getGrade(),
                l.getStartType(), l.getLinePath(), l.getSortOrder()
        )).toList();
        return new SchoolBlock(
                e.getId(), e.getSchoolId(), e.getType(), e.getName(),
                e.getLat(), e.getLon(), e.getPhotoPath(), e.getDescription(),
                e.getCreatedByUid(), e.getCreatedAt(), lines
        );
    }
}
