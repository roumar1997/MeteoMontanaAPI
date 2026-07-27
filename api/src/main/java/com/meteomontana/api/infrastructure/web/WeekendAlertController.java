package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.application.alerts.WeekendAlertPrefsUseCase;
import com.meteomontana.api.domain.model.AlertPreference;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Preferencias de la "alerta del finde": GET/PUT /api/me/weekend-alert.
 * Solo mapea DTO↔dominio; la validación y las reglas de compatibilidad con
 * apps antiguas viven en {@link WeekendAlertPrefsUseCase}.
 */
@RestController
@RequestMapping("/api/me/weekend-alert")
public class WeekendAlertController {

    /**
     * mode: SCHOOLS (ids elegidos) o NEARBY (radio km desde lat/lon del usuario).
     * alertDays: días ISO-8601 (1=lunes .. 7=domingo) que se comparan en el aviso.
     * optimalEnabled/optimalThreshold: alerta "ventana óptima hoy" sobre las
     * favoritas. Nullables para que apps antiguas que no los mandan no los pisen.
     */
    public record WeekendAlertDto(boolean enabled, int notifyDay, int notifyHour,
                                  List<String> schoolIds, String mode,
                                  Integer radiusKm, Double lat, Double lon,
                                  List<Integer> alertDays,
                                  Boolean optimalEnabled, Integer optimalThreshold) {}

    private final WeekendAlertPrefsUseCase useCase;

    public WeekendAlertController(WeekendAlertPrefsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public WeekendAlertDto get(@AuthenticationPrincipal FirebaseUser user) {
        return toDto(useCase.get(user.uid()));
    }

    @PutMapping
    public WeekendAlertDto put(@AuthenticationPrincipal FirebaseUser user,
                               @RequestBody WeekendAlertDto dto) {
        return toDto(useCase.update(user.uid(), dto.enabled(), dto.notifyDay(), dto.notifyHour(),
                dto.schoolIds(), dto.mode(), dto.radiusKm(), dto.lat(), dto.lon(),
                dto.alertDays(), dto.optimalEnabled(), dto.optimalThreshold()));
    }

    private static WeekendAlertDto toDto(AlertPreference p) {
        return new WeekendAlertDto(p.enabled(), p.notifyDay(), p.notifyHour(),
                splitCsv(p.schoolIds()), p.mode(), p.radiusKm(), p.userLat(), p.userLon(),
                WeekendAlertPrefsUseCase.parseDays(p.alertDays()),
                p.optimalEnabled(), p.optimalThreshold());
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).filter(s -> !s.isBlank()).toList();
    }
}
