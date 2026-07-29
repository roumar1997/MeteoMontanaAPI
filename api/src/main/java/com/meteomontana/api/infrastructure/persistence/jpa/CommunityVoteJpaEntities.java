package com.meteomontana.api.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Entidades JPA de los votos comunitarios (V60). */
public final class CommunityVoteJpaEntities {

    private CommunityVoteJpaEntities() {}

    @Entity
    @Table(name = "block_orientation_votes")
    public static class OrientationVoteJpaEntity {
        @Id
        private String id;
        @Column(name = "block_id", nullable = false)
        private String blockId;
        @Column(name = "photo_index")
        private Integer photoIndex;
        @Column(name = "voter_uid", nullable = false)
        private String voterUid;
        @Column(nullable = false, length = 2)
        private String aspect;
        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;

        protected OrientationVoteJpaEntity() {}

        public OrientationVoteJpaEntity(String id, String blockId, Integer photoIndex,
                                        String voterUid, String aspect, LocalDateTime createdAt) {
            this.id = id; this.blockId = blockId; this.photoIndex = photoIndex;
            this.voterUid = voterUid; this.aspect = aspect; this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getBlockId() { return blockId; }
        public Integer getPhotoIndex() { return photoIndex; }
        public String getVoterUid() { return voterUid; }
        public String getAspect() { return aspect; }
        public void setAspect(String aspect) { this.aspect = aspect; }
    }

    @Entity
    @Table(name = "line_grade_votes")
    public static class GradeVoteJpaEntity {
        @Id
        private String id;
        @Column(name = "line_id", nullable = false)
        private String lineId;
        @Column(name = "voter_uid", nullable = false)
        private String voterUid;
        @Column(nullable = false, length = 8)
        private String grade;
        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;

        protected GradeVoteJpaEntity() {}

        public GradeVoteJpaEntity(String id, String lineId, String voterUid,
                                  String grade, LocalDateTime createdAt) {
            this.id = id; this.lineId = lineId; this.voterUid = voterUid;
            this.grade = grade; this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getLineId() { return lineId; }
        public String getVoterUid() { return voterUid; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
    }
}
