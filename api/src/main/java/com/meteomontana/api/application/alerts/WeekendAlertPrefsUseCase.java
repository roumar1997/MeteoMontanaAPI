package com.meteomontana.api.application.alerts;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.model.AlertPreference;
import com.meteomontana.api.domain.port.AlertPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Leer y guardar las preferencias de alerta del propio usuario
 * (GET/PUT /api/me/weekend-alert). La validación y la regla "apps antiguas
 * no mandan campos nuevos → conservar lo que hubiera" viven aquí, no en el
 * controller (que solo mapea DTO↔dominio).
 */
@Service
@RequiredArgsConstructor
public class WeekendAlertPrefsUseCase {

    /** Días por defecto del aviso: vie/sáb/dom (formato ISO 1=lunes..7=domingo). */
    private static final String DEFAULT_DAYS = "5,6,7";

    private final AlertPreferenceRepository repository;

    /** Preferencias del usuario, con los defaults si nunca configuró nada. */
    public AlertPreference get(String uid) {
        return repository.findByUid(uid).orElse(defaults(uid));
    }

    /**
     * Valida y guarda. optimalEnabled/optimalThreshold nulos = la app es
     * antigua y no los manda → se conserva el valor previo.
     */
    public AlertPreference update(String uid, boolean enabled, int notifyDay, int notifyHour,
                                  List<String> schoolIds, String mode,
                                  Integer radiusKm, Double lat, Double lon,
                                  List<Integer> alertDays,
                                  Boolean optimalEnabled, Integer optimalThreshold) {
        boolean nearby = "NEARBY".equalsIgnoreCase(mode);
        // Validamos escuelas/radio solo si la alerta de tiempo está activa:
        // un usuario puede activar únicamente la alerta de ventana óptima sin
        // haber configurado nunca la de tiempo.
        if (enabled) {
            if (nearby) {
                if (radiusKm == null || radiusKm < 1 || radiusKm > 500)
                    throw new BadRequestException("radiusKm debe ser 1-500");
                if (lat == null || lon == null)
                    throw new BadRequestException("Falta tu ubicación para el modo cercanía");
            } else {
                if (schoolIds == null || schoolIds.isEmpty() || schoolIds.size() > 3)
                    throw new BadRequestException("Elige entre 1 y 3 escuelas");
            }
        }
        if (optimalThreshold != null && (optimalThreshold < 1 || optimalThreshold > 100))
            throw new BadRequestException("optimalThreshold debe ser 1-100");
        // Apps antiguas no mandan alertDays → seguimos con vie/sáb/dom.
        List<Integer> days = (alertDays == null || alertDays.isEmpty())
                ? parseDays(DEFAULT_DAYS)
                : alertDays.stream().distinct().sorted().toList();
        if (days.stream().anyMatch(d -> d < 1 || d > 7))
            throw new BadRequestException("alertDays deben ser 1-7");
        if (notifyDay < 1 || notifyDay > 7)
            throw new BadRequestException("notifyDay debe ser 1-7");
        if (notifyHour < 0 || notifyHour > 23)
            throw new BadRequestException("notifyHour debe ser 0-23");

        AlertPreference previous = get(uid);
        AlertPreference updated = new AlertPreference(
                uid, enabled, notifyDay, notifyHour,
                nearby || schoolIds == null ? null : String.join(",", schoolIds),
                nearby ? "NEARBY" : "SCHOOLS",
                nearby ? radiusKm : null,
                nearby ? lat : null,
                nearby ? lon : null,
                days.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(DEFAULT_DAYS),
                optimalEnabled != null ? optimalEnabled : previous.optimalEnabled(),
                optimalThreshold != null ? optimalThreshold : previous.optimalThreshold(),
                previous.optimalLastSent());
        repository.save(updated);
        return updated;
    }

    /** Preferencias por defecto de quien nunca ha configurado la alerta. */
    public static AlertPreference defaults(String uid) {
        return new AlertPreference(uid, false, 4, 20, "", "SCHOOLS",
                null, null, null, DEFAULT_DAYS, false, 70, null);
    }

    /** CSV "5,6,7" → [5,6,7]; null/vacío → vie/sáb/dom. */
    public static List<Integer> parseDays(String csv) {
        if (csv == null || csv.isBlank()) return List.of(5, 6, 7);
        return Arrays.stream(csv.split(",")).filter(s -> !s.isBlank())
                .map(String::trim).map(Integer::parseInt).toList();
    }
}
