package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.FeedPost;
import com.meteomontana.api.domain.port.FeedPostRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaFeedPostRepositoryAdapter implements FeedPostRepository {

    private final SpringDataFeedPostRepository jpaRepo;

    public JpaFeedPostRepositoryAdapter(SpringDataFeedPostRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<FeedPost> pageAllPublic(long before, int limit) {
        return jpaRepo.pageAllPublic(before, limit).stream().map(this::toDomain).toList();
    }

    @Override
    public List<FeedPost> pageByAuthors(List<String> authorUids, long before, int limit) {
        return jpaRepo.pageByAuthors(authorUids, before, limit).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FeedPost> findById(long id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsById(long id) { return jpaRepo.existsById(id); }

    @Override
    public Optional<FeedPost> findByUserLineAndKind(String userUid, String lineId, String kind) {
        return jpaRepo.findByUserUidAndLineIdAndKind(userUid, lineId, kind).map(this::toDomain);
    }

    @Override
    public List<FeedPost> findRecentByKinds(Collection<String> kinds, LocalDateTime since) {
        return jpaRepo.findByKindInAndCreatedAtAfterOrderByCreatedAtDesc(kinds, since)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public FeedPost create(FeedPost post) {
        FeedPostJpaEntity e = new FeedPostJpaEntity(
                post.getUserUid(), post.getSchoolId(), post.getSchoolName(),
                post.getBlockId(), post.getBlockName(),
                post.getLineId(), post.getLineName(),
                post.getGrade(), post.getKind());
        e.setDiscipline(post.getDiscipline());
        e.setRockType(post.getRockType());
        e.setCaption(post.getCaption());
        e.setPhotoPath(post.getPhotoPath());
        return toDomain(jpaRepo.save(e));
    }

    @Override
    public void updatePhotoPath(long postId, String photoPath) {
        jpaRepo.findById(postId).ifPresent(e -> {
            e.setPhotoPath(photoPath);
            jpaRepo.save(e);
        });
    }

    @Override
    public void deleteById(long postId) { jpaRepo.deleteById(postId); }

    private FeedPost toDomain(FeedPostJpaEntity e) {
        FeedPost p = new FeedPost(e.getId(), e.getUserUid(), e.getSchoolId(), e.getSchoolName(),
                e.getBlockId(), e.getBlockName(), e.getLineId(), e.getLineName(),
                e.getGrade(), e.getKind(), e.getCreatedAt());
        p.setDiscipline(e.getDiscipline());
        p.setRockType(e.getRockType());
        p.setCaption(e.getCaption());
        p.setPhotoPath(e.getPhotoPath());
        return p;
    }
}
