package com.meteomontana.api.infrastructure.persistence.jpa;

import com.meteomontana.api.domain.model.Approach;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Table(name = "approaches")
@Getter
public class ApproachJpaEntity {

    @Id
    private String id;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(name = "from_block_id")
    private String fromBlockId;

    @Column(name = "to_block_id")
    private String toBlockId;

    private String name;

    @Column(name = "path_json", columnDefinition = "TEXT", nullable = false)
    private String pathJson;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "ascent_m")
    private Integer ascentM;

    @Column(name = "duration_min")
    private Integer durationMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Approach.Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Approach.Status status;

    @Column(name = "author_uid", nullable = false)
    private String authorUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "approach", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("positionIdx ASC")
    private List<ApproachPinJpaEntity> pins = new ArrayList<>();

    protected ApproachJpaEntity() {}

    public ApproachJpaEntity(String id, String schoolId, String fromBlockId, String toBlockId,
                              String name, String pathJson, Integer distanceM, Integer ascentM,
                              Integer durationMin, Approach.Source source, Approach.Status status,
                              String authorUid, LocalDateTime createdAt) {
        this.id = id; this.schoolId = schoolId; this.fromBlockId = fromBlockId;
        this.toBlockId = toBlockId; this.name = name; this.pathJson = pathJson;
        this.distanceM = distanceM; this.ascentM = ascentM; this.durationMin = durationMin;
        this.source = source; this.status = status; this.authorUid = authorUid;
        this.createdAt = createdAt;
    }

    public void addPin(ApproachPinJpaEntity pin) {
        pin.setApproach(this);
        pins.add(pin);
    }
}
