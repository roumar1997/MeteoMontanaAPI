package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.SchoolPhoto;
import com.meteomontana.api.domain.port.SchoolPhotoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSchoolPhotoRepositoryAdapter implements SchoolPhotoRepository {

    private final SpringDataSchoolPhotoRepository jpaRepo;
    private final SpringDataSchoolRepository schoolJpaRepo;

    public JpaSchoolPhotoRepositoryAdapter(SpringDataSchoolPhotoRepository jpaRepo,
                                           SpringDataSchoolRepository schoolJpaRepo) {
        this.jpaRepo = jpaRepo;
        this.schoolJpaRepo = schoolJpaRepo;
    }

    @Override
    public List<SchoolPhoto> findBySchoolId(String schoolId) {
        return jpaRepo.findBySchoolIdOrderByCreatedAtDesc(schoolId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SchoolPhoto> findById(String id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public SchoolPhoto save(SchoolPhoto photo) {
        SchoolJpaEntity school = schoolJpaRepo.findById(photo.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("School not found: " + photo.getSchoolId()));

        SchoolPhotoJpaEntity entity = new SchoolPhotoJpaEntity(
                photo.getId(),
                school,
                photo.getStoragePath(),
                photo.getUploadedByUid(),
                photo.getCaption(),
                photo.getWidth(),
                photo.getHeight(),
                photo.getSizeBytes(),
                photo.getContentType(),
                photo.getCreatedAt()
        );
        return toDomain(jpaRepo.save(entity));
    }

    @Override
    public void deleteById(String id) {
        jpaRepo.deleteById(id);
    }

    private SchoolPhoto toDomain(SchoolPhotoJpaEntity e) {
        return new SchoolPhoto(
                e.getId(),
                e.getSchool().getId(),
                e.getStoragePath(),
                e.getUploadedByUid(),
                e.getCaption(),
                e.getWidth(),
                e.getHeight(),
                e.getSizeBytes(),
                e.getContentType(),
                e.getCreatedAt()
        );
    }
}
