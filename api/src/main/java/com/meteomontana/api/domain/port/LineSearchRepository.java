package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.LineSearchHit;

import java.util.List;

/** Búsqueda por nombre de vías y de piedras/muros (puerto de dominio). */
public interface LineSearchRepository {

    /** Vías cuyo nombre contiene la query (sin distinguir mayúsculas). */
    List<LineSearchHit> searchLinesByName(String query, int limit);

    /** Piedras/muros (type BLOCK) cuyo nombre contiene la query. */
    List<LineSearchHit> searchBlocksByName(String query, int limit);
}
