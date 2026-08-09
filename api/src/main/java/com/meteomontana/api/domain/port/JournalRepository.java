package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.JournalSession;

import java.util.List;
import java.util.Optional;

public interface JournalRepository {
    JournalSession save(JournalSession session);
    Optional<JournalSession> findById(String id);
    List<JournalSession> findByUid(String uid);
    void deleteById(String id);

    /** Cambia la fecha de una entrada (el diario cuenta cuándo la hiciste). */
    void updateSessionDate(String id, java.time.LocalDate newDate);

    /**
     * Propaga el nombre nuevo de una vía a todas las entradas que la tienen
     * marcada. El diario guarda el nombre copiado (para listar sin JOIN), así
     * que al renombrar una vía hay que traerlo o el perfil de todo el mundo se
     * queda con el nombre viejo — y deja de llevar a su piedra al pulsarlo.
     *
     * @return cuántas entradas se han actualizado.
     */
    int updateNameByLineId(String lineId, String name);
}
