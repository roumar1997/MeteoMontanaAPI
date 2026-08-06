package com.meteomontana.api.application.radar;

import com.meteomontana.api.infrastructure.radar.AemetRadarClient;
import com.meteomontana.api.infrastructure.radar.SpringDataRadarFrameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Los GIF del radar viven dentro de Postgres, así que la retención es lo que
 * decide cuánto ocupa la base de datos: con 48h la tabla llegó a 341 MB de un
 * volumen de 500 y el disco se puso al 97%. La ventana es configurable por
 * entorno para poder ampliarla cuando las imágenes se muden a R2.
 */
class RadarRetentionTest {

    private SpringDataRadarFrameRepository repo;
    private RadarCollector collector;

    @BeforeEach void setUp() {
        repo = mock(SpringDataRadarFrameRepository.class);
        collector = new RadarCollector(mock(AemetRadarClient.class), repo);
    }

    private LocalDateTime corteAlPurgarCon(int horas) {
        ReflectionTestUtils.setField(collector, "retentionHours", horas);
        LocalDateTime antes = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        collector.prune();
        ArgumentCaptor<LocalDateTime> corte = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).deleteOlderThan(corte.capture());
        return corte.getValue().plusHours(horas);   // reconstruye el "ahora" usado
    }

    @Test void purgaConLaVentanaConfigurada() {
        LocalDateTime ahoraUsado = corteAlPurgarCon(24);
        // El corte tiene que ser "ahora menos 24h", no otra cosa: se comprueba
        // reconstruyendo el ahora y viendo que cae en este mismo instante.
        assertThat(Duration.between(ahoraUsado, LocalDateTime.now(ZoneId.of("Europe/Madrid"))).abs())
                .isLessThan(Duration.ofMinutes(1));
    }

    @Test void unaVentanaMayorConservaMas() {
        ReflectionTestUtils.setField(collector, "retentionHours", 24);
        collector.prune();
        ArgumentCaptor<LocalDateTime> c24 = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).deleteOlderThan(c24.capture());

        reset(repo);
        ReflectionTestUtils.setField(collector, "retentionHours", 48);
        collector.prune();
        ArgumentCaptor<LocalDateTime> c48 = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).deleteOlderThan(c48.capture());

        // Más retención = corte más antiguo = se borra menos.
        assertThat(c48.getValue()).isBefore(c24.getValue());
    }

    @Test void sinConfigurarNadaSonVeinticuatroHoras() throws Exception {
        // Lee el default REAL de la anotación: si alguien lo sube sin querer,
        // la base de datos vuelve a crecer hasta llenar el disco.
        var campo = RadarCollector.class.getDeclaredField("retentionHours");
        var valor = campo.getAnnotation(
                org.springframework.beans.factory.annotation.Value.class).value();
        assertThat(valor).isEqualTo("${radar.retention-hours:24}");
    }
}
