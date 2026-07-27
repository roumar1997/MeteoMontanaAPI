package com.meteomontana.api.domain.port;

import com.meteomontana.api.domain.model.AlertPreference;

import java.time.LocalDate;
import java.util.List;

/** Preferencias de alertas de los usuarios (puerto de dominio). */
public interface AlertPreferenceRepository {

    /** Usuarios con el aviso de fin de semana activo para ese día y hora. */
    List<AlertPreference> findEnabledFor(int notifyDay, int notifyHour);

    /** Usuarios con la alerta de "ventana óptima" activa. */
    List<AlertPreference> findOptimalEnabled();

    /** Marca la fecha del último aviso de ventana óptima (anti-repetición). */
    void markOptimalSent(String uid, LocalDate day);
}
