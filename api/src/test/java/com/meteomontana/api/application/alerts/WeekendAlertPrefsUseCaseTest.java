package com.meteomontana.api.application.alerts;

import com.meteomontana.api.domain.exception.BadRequestException;
import com.meteomontana.api.domain.model.AlertPreference;
import com.meteomontana.api.domain.port.AlertPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La validación y la compatibilidad con apps antiguas que ANTES vivían sin
 * tests en el controller. Contratos clave: campos nuevos nulos no pisan lo
 * guardado, y el modo NEARBY/SCHOOLS limpia los campos del otro modo.
 */
class WeekendAlertPrefsUseCaseTest {

    private AlertPreferenceRepository repo;
    private WeekendAlertPrefsUseCase useCase;

    @BeforeEach
    void setUp() {
        repo = mock(AlertPreferenceRepository.class);
        useCase = new WeekendAlertPrefsUseCase(repo);
    }

    @Test
    void getDevuelveDefaultsSiNuncaConfiguro() {
        when(repo.findByUid("u1")).thenReturn(Optional.empty());
        AlertPreference p = useCase.get("u1");
        assertThat(p.enabled()).isFalse();
        assertThat(p.optimalThreshold()).isEqualTo(70);
        assertThat(WeekendAlertPrefsUseCase.parseDays(p.alertDays())).containsExactly(5, 6, 7);
    }

    @Test
    void modoSchoolsExigeEntre1y3Escuelas() {
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 20,
                List.of(), "SCHOOLS", null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 20,
                List.of("a", "b", "c", "d"), "SCHOOLS", null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void modoNearbyExigeRadioYUbicacion() {
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 20,
                null, "NEARBY", 900, 40.0, -3.7, null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 20,
                null, "NEARBY", 50, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void appAntiguaSinCamposNuevosConservaLoGuardado() {
        // Guardado previo: ventana óptima activada con umbral 85.
        when(repo.findByUid("u1")).thenReturn(Optional.of(new AlertPreference(
                "u1", true, 4, 20, "zarzalejo", "SCHOOLS", null, null, null,
                "5,6,7", true, 85, null)));
        // La app vieja manda optimalEnabled/optimalThreshold = null.
        AlertPreference saved = useCase.update("u1", true, 4, 20,
                List.of("zarzalejo"), "SCHOOLS", null, null, null, null, null, null);
        assertThat(saved.optimalEnabled()).isTrue();
        assertThat(saved.optimalThreshold()).isEqualTo(85);
        verify(repo).save(any());
    }

    @Test
    void cambiarAScoolsLimpiaLosCamposDeNearbyYViceversa() {
        when(repo.findByUid("u1")).thenReturn(Optional.empty());
        ArgumentCaptor<AlertPreference> captor = ArgumentCaptor.forClass(AlertPreference.class);

        useCase.update("u1", true, 4, 20, null, "NEARBY", 50, 40.0, -3.7, null, null, null);
        verify(repo).save(captor.capture());
        AlertPreference nearby = captor.getValue();
        assertThat(nearby.schoolIds()).isNull();
        assertThat(nearby.radiusKm()).isEqualTo(50);

        useCase.update("u1", true, 4, 20, List.of("zarzalejo"), "SCHOOLS",
                99, 1.0, 1.0, null, null, null);
        verify(repo, org.mockito.Mockito.times(2)).save(captor.capture());
        AlertPreference schools = captor.getValue();
        assertThat(schools.radiusKm()).isNull();
        assertThat(schools.userLat()).isNull();
        assertThat(schools.schoolIds()).isEqualTo("zarzalejo");
    }

    @Test
    void diasInvalidosYHoraInvalidaRechazan() {
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 20,
                List.of("z"), "SCHOOLS", null, null, null, List.of(0, 8), null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> useCase.update("u1", true, 4, 24,
                List.of("z"), "SCHOOLS", null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class);
    }
}
