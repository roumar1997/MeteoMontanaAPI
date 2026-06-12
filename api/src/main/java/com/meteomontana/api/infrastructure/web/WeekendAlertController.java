package com.meteomontana.api.infrastructure.web;

import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataWeekendAlertRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.WeekendAlertPrefJpaEntity;
import com.meteomontana.api.infrastructure.security.FirebaseUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Preferencias de la "alerta del finde": GET/PUT /api/me/weekend-alert. */
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

    private final SpringDataWeekendAlertRepository repository;

    public WeekendAlertController(SpringDataWeekendAlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public WeekendAlertDto get(@AuthenticationPrincipal FirebaseUser user) {
        return repository.findById(user.uid())
                .map(e -> new WeekendAlertDto(e.isEnabled(), e.getNotifyDay(), e.getNotifyHour(),
                        e.getSchoolIds() == null ? List.of()
                            : Arrays.stream(e.getSchoolIds().split(",")).filter(s -> !s.isBlank()).toList(),
                        e.getMode(), e.getRadiusKm(), e.getUserLat(), e.getUserLon(),
                        parseDays(e.getAlertDays()),
                        e.isOptimalEnabled(), e.getOptimalThreshold()))
                .orElse(new WeekendAlertDto(false, 4, 20, List.of(), "SCHOOLS", null, null, null,
                        List.of(5, 6, 7), false, 70));
    }

    @PutMapping
    public WeekendAlertDto put(@AuthenticationPrincipal FirebaseUser user,
                               @RequestBody WeekendAlertDto dto) {
        boolean nearby = "NEARBY".equalsIgnoreCase(dto.mode());
        // Validamos escuelas/radio solo si la alerta de tiempo está activa:
        // un usuario puede activar únicamente la alerta de ventana óptima sin
        // haber configurado nunca la de tiempo.
        if (dto.enabled()) {
            if (nearby) {
                if (dto.radiusKm() == null || dto.radiusKm() < 1 || dto.radiusKm() > 500)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "radiusKm debe ser 1-500");
                if (dto.lat() == null || dto.lon() == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta tu ubicación para el modo cercanía");
            } else {
                if (dto.schoolIds() == null || dto.schoolIds().isEmpty() || dto.schoolIds().size() > 3)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige entre 1 y 3 escuelas");
            }
        }
        if (dto.optimalThreshold() != null
                && (dto.optimalThreshold() < 1 || dto.optimalThreshold() > 100))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "optimalThreshold debe ser 1-100");
        // Apps antiguas no mandan alertDays → seguimos con vie/sáb/dom.
        List<Integer> days = (dto.alertDays() == null || dto.alertDays().isEmpty())
                ? List.of(5, 6, 7)
                : dto.alertDays().stream().distinct().sorted().toList();
        if (days.stream().anyMatch(d -> d < 1 || d > 7))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alertDays deben ser 1-7");
        if (dto.notifyDay() < 1 || dto.notifyDay() > 7)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notifyDay debe ser 1-7");
        if (dto.notifyHour() < 0 || dto.notifyHour() > 23)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notifyHour debe ser 0-23");

        var entity = repository.findById(user.uid())
                .orElse(new WeekendAlertPrefJpaEntity(user.uid(), true, 4, 20, "", LocalDateTime.now()));
        entity.setEnabled(dto.enabled());
        entity.setNotifyDay(dto.notifyDay());
        entity.setNotifyHour(dto.notifyHour());
        entity.setMode(nearby ? "NEARBY" : "SCHOOLS");
        entity.setRadiusKm(nearby ? dto.radiusKm() : null);
        entity.setUserLat(nearby ? dto.lat() : null);
        entity.setUserLon(nearby ? dto.lon() : null);
        entity.setSchoolIds(nearby || dto.schoolIds() == null ? null : String.join(",", dto.schoolIds()));
        entity.setAlertDays(days.stream().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("5,6,7"));
        // Apps antiguas no mandan estos campos → conservamos lo que hubiera.
        if (dto.optimalEnabled() != null) entity.setOptimalEnabled(dto.optimalEnabled());
        if (dto.optimalThreshold() != null) entity.setOptimalThreshold(dto.optimalThreshold());
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return new WeekendAlertDto(dto.enabled(), dto.notifyDay(), dto.notifyHour(),
                dto.schoolIds(), entity.getMode(), entity.getRadiusKm(),
                entity.getUserLat(), entity.getUserLon(), days,
                entity.isOptimalEnabled(), entity.getOptimalThreshold());
    }

    public static List<Integer> parseDays(String csv) {
        if (csv == null || csv.isBlank()) return List.of(5, 6, 7);
        return Arrays.stream(csv.split(",")).filter(s -> !s.isBlank())
                .map(String::trim).map(Integer::parseInt).toList();
    }
}
