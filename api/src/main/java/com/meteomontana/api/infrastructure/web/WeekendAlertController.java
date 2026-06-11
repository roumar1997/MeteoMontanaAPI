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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Preferencias de la "alerta del finde": GET/PUT /api/me/weekend-alert. */
@RestController
@RequestMapping("/api/me/weekend-alert")
public class WeekendAlertController {

    public record WeekendAlertDto(boolean enabled, int notifyDay, int notifyHour, List<String> schoolIds) {}

    private final SpringDataWeekendAlertRepository repository;

    public WeekendAlertController(SpringDataWeekendAlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public WeekendAlertDto get(@AuthenticationPrincipal FirebaseUser user) {
        return repository.findById(user.uid())
                .map(e -> new WeekendAlertDto(e.isEnabled(), e.getNotifyDay(), e.getNotifyHour(),
                        Arrays.stream(e.getSchoolIds().split(",")).filter(s -> !s.isBlank()).toList()))
                .orElse(new WeekendAlertDto(false, 4, 20, List.of()));
    }

    @PutMapping
    public WeekendAlertDto put(@AuthenticationPrincipal FirebaseUser user,
                               @RequestBody WeekendAlertDto dto) {
        if (dto.schoolIds() == null || dto.schoolIds().isEmpty() || dto.schoolIds().size() > 3)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige entre 1 y 3 escuelas");
        if (dto.notifyDay() < 1 || dto.notifyDay() > 7)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notifyDay debe ser 1-7");
        if (dto.notifyHour() < 0 || dto.notifyHour() > 23)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "notifyHour debe ser 0-23");

        var entity = repository.findById(user.uid())
                .orElse(new WeekendAlertPrefJpaEntity(user.uid(), true, 4, 20, "", Instant.now()));
        entity.setEnabled(dto.enabled());
        entity.setNotifyDay(dto.notifyDay());
        entity.setNotifyHour(dto.notifyHour());
        entity.setSchoolIds(String.join(",", dto.schoolIds()));
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        return dto;
    }
}
