package com.meteomontana.api.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/** Plantilla de entreno de agarres, guardada para reutilizar. */
public class GripWorkout {
    private final String id;
    private final String uid;
    private final String name;
    private final String handMode;            // UNA | POR_SERIE | POR_REP
    private final String countMode;           // TIEMPO | PESO
    private final int restBetweenSetsS;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<GripWorkoutSet> sets;

    public GripWorkout(String id, String uid, String name, String handMode, String countMode,
                        int restBetweenSetsS, LocalDateTime createdAt, LocalDateTime updatedAt,
                        List<GripWorkoutSet> sets) {
        this.id = id;
        this.uid = uid;
        this.name = name;
        this.handMode = handMode;
        this.countMode = countMode;
        this.restBetweenSetsS = restBetweenSetsS;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sets = sets;
    }

    public String getId()                  { return id; }
    public String getUid()                 { return uid; }
    public String getName()                { return name; }
    public String getHandMode()            { return handMode; }
    public String getCountMode()           { return countMode; }
    public int getRestBetweenSetsS()       { return restBetweenSetsS; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getUpdatedAt()    { return updatedAt; }
    public List<GripWorkoutSet> getSets()  { return sets; }
}
