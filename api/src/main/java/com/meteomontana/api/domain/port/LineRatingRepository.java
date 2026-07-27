package com.meteomontana.api.domain.port;

import java.util.Collection;
import java.util.Map;

/** Valoraciones por estrellas de las vías (puerto de dominio). */
public interface LineRatingRepository {

    /** Mis estrellas en esa vía (0 si no he votado). */
    int starsOf(String uid, String lineId);

    /** Fija mis estrellas (crea o actualiza). */
    void setStars(String uid, String lineId, int stars);

    /** Retira mi valoración. */
    void removeRating(String uid, String lineId);

    /** Media de estrellas de la vía (null si no tiene ninguna). */
    Double avgStars(String lineId);

    long countRatings(String lineId);

    /** Medias en batch para una lista de vías (sin N+1). */
    Map<String, Double> avgStarsByLineIds(Collection<String> lineIds);

    /** Mis estrellas en batch para una lista de vías. */
    Map<String, Integer> myStarsByLineIds(String uid, Collection<String> lineIds);
}
