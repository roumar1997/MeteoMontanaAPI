package com.meteomontana.api.domain.model;

/** Combinación agarre = dedos/posición × estilo. Catálogo fijo sembrado por migración. */
public record GripType(int id, String fingerGroup, String style) {
    // fingerGroup: FIVE | FOUR | THREE | FRONT_TWO | MID_TWO
    // style: CRIMP | HALF_CRIMP | DRAG
}
