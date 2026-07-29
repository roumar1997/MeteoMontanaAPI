package com.meteomontana.api.domain.score;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La física del sol para Zarzalejo (40.54N, -4.11O, verano CEST = UTC+2).
 * Referencias redondas conocidas: al mediodía solar el sol está al SUR y alto;
 * amanece por el ESTE y se pone por el OESTE.
 */
class SunPositionCalculatorTest {

    private static final double LAT = 40.54, LON = -4.11;
    private static final int CEST = 2 * 3600;

    @Test
    void alMediodiaSolarElSolEstaAlSurYAlto() {
        // Mediodía solar en Madrid en verano ≈ 14:10 hora oficial.
        LocalDateTime t = LocalDateTime.of(2026, 7, 29, 14, 10);
        double az = SunPositionCalculator.azimuthDeg(t, LAT, LON, CEST);
        double el = SunPositionCalculator.elevationDeg(t, LAT, LON, CEST);
        assertThat(az).isBetween(160.0, 200.0);
        assertThat(el).isGreaterThan(60.0);
    }

    @Test
    void porLaMananaElSolEstaAlEsteYPorLaTardeAlOeste() {
        double azManana = SunPositionCalculator.azimuthDeg(
                LocalDateTime.of(2026, 7, 29, 9, 0), LAT, LON, CEST);
        double azTarde = SunPositionCalculator.azimuthDeg(
                LocalDateTime.of(2026, 7, 29, 19, 0), LAT, LON, CEST);
        assertThat(azManana).isBetween(60.0, 120.0);
        assertThat(azTarde).isBetween(250.0, 300.0);
    }

    @Test
    void deNocheNingunaParedRecibeSol() {
        LocalDateTime noche = LocalDateTime.of(2026, 7, 29, 23, 30);
        for (String aspect : new String[]{"N", "NE", "E", "SE", "S", "SO", "O", "NO"}) {
            assertThat(SunPositionCalculator.isWallInSun(aspect, noche, LAT, LON, CEST))
                    .as("pared %s de noche", aspect).isFalse();
        }
    }

    @Test
    void unaParedSuroesteCogeElSolDeTardeYNoElDeManana() {
        assertThat(SunPositionCalculator.isWallInSun("SO",
                LocalDateTime.of(2026, 7, 29, 18, 0), LAT, LON, CEST)).isTrue();
        assertThat(SunPositionCalculator.isWallInSun("SO",
                LocalDateTime.of(2026, 7, 29, 9, 0), LAT, LON, CEST)).isFalse();
    }

    @Test
    void unaParedNorteCasiNuncaVeElSolDirectoEnInvierno() {
        // Enero: el sol nunca pasa del sureste-suroeste en Madrid.
        for (int h = 9; h <= 17; h++) {
            assertThat(SunPositionCalculator.isWallInSun("N",
                    LocalDateTime.of(2026, 1, 15, h, 0), LAT, LON, 3600))
                    .as("pared N enero %dh", h).isFalse();
        }
    }

    @Test
    void rumbosInvalidosNoRompen() {
        assertThat(SunPositionCalculator.aspectToDegrees("XX")).isNull();
        assertThat(SunPositionCalculator.aspectToDegrees(null)).isNull();
        assertThat(SunPositionCalculator.isWallInSun(null,
                LocalDateTime.of(2026, 7, 29, 12, 0), LAT, LON, CEST)).isFalse();
    }
}
