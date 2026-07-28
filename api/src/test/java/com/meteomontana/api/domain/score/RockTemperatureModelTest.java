package com.meteomontana.api.domain.score;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El caso que motivó la feature (Rodrigo, 2026-07-28): "si toda la tarde hizo
 * 35° al sol en Zarzalejo y de repente se va el sol, la roca no se pone fría
 * de repente". La roca tiene memoria; estos tests fijan esa física.
 */
class RockTemperatureModelTest {

    private static final double GRANITO_TAU =
            RockThermalProfile.forRockType("granito").tauHours();
    private static final double ARENISCA_TAU =
            RockThermalProfile.forRockType("arenisca").tauHours();

    /** Tarde a 35° con sol fuerte; a las 19h el aire cae a 24° y se va el sol. */
    private static List<Double> tardeCalurosa() {
        List<Double> t = new ArrayList<>();
        for (int h = 0; h < 24; h++) t.add(h < 19 ? 35.0 : 24.0);
        return t;
    }

    private static List<Double> solHasta19() {
        List<Double> r = new ArrayList<>();
        for (int h = 0; h < 24; h++) r.add(h >= 10 && h < 19 ? 800.0 : 0.0);
        return r;
    }

    private static List<Double> calma() {
        return Collections.nCopies(24, 5.0);
    }

    @Test
    void elGranitoSigueCalienteUnParDeHorasTrasPerderElSol() {
        double[] rock = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), calma(), GRANITO_TAU);
        // A las 18h (35° + sol) la roca está claramente por ENCIMA del aire.
        assertThat(rock[18]).isGreaterThan(38.0);
        // A las 20h el aire ya está a 24°... pero la roca sigue >29°.
        assertThat(rock[20]).isGreaterThan(29.0);
        // Y solo hacia las 23h se acerca de verdad al aire.
        assertThat(rock[23]).isLessThan(28.0);
    }

    @Test
    void laAreniscaSueltaElCalorMuchoAntesQueElGranito() {
        double[] granito = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), calma(), GRANITO_TAU);
        double[] arenisca = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), calma(), ARENISCA_TAU);
        // Dos horas después de perder el sol, la arenisca va bastante más fría.
        assertThat(arenisca[21]).isLessThan(granito[21] - 2.0);
    }

    @Test
    void elVientoAceleraElEnfriamiento() {
        List<Double> ventoso = Collections.nCopies(24, 35.0);
        double[] conCalma = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), calma(), GRANITO_TAU);
        double[] conViento = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), ventoso, GRANITO_TAU);
        assertThat(conViento[21]).isLessThan(conCalma[21] - 1.5);
    }

    @Test
    void mananaDeInviernoLaRocaSigueHeladaAunqueElAireYaEsteBien() {
        // Noche a -2°; a las 9h el aire sube de golpe a 12° (día soleado suave).
        List<Double> aire = new ArrayList<>();
        for (int h = 0; h < 24; h++) aire.add(h < 9 ? -2.0 : 12.0);
        double[] rock = RockTemperatureModel.estimate(
                aire, Collections.nCopies(24, 0.0), calma(), GRANITO_TAU);
        // A las 10h el aire dice 12° pero la roca sigue por debajo de 7°.
        assertThat(rock[10]).isLessThan(7.0);
    }

    @Test
    void sinDatosDeRadiacionDegradaARocaIgualQueAire() {
        // Caché antigua (radiation == null): sin sol modelado y aire CONSTANTE
        // → la roca converge al aire y el ajuste del score queda en 0.
        List<Double> aire = Collections.nCopies(24, 20.0);
        double[] rock = RockTemperatureModel.estimate(aire, null, null, GRANITO_TAU);
        assertThat(rock[23]).isCloseTo(20.0, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void serieVaciaNoRevienta() {
        assertThat(RockTemperatureModel.estimate(List.of(), null, null, GRANITO_TAU)).isEmpty();
        assertThat(RockTemperatureModel.estimate(null, null, null, GRANITO_TAU)).isEmpty();
    }

    // ── El ajuste del score con estas series ──────────────────────────────────

    @Test
    void elScoreDeLas20hCastigaLaRocaCalienteAunqueElAireDigaQueBien() {
        double[] rock = RockTemperatureModel.estimate(
                tardeCalurosa(), solHasta19(), calma(), GRANITO_TAU);
        // Aire 24° / roca ~30° a las 20h → ajuste negativo fuerte.
        int adjust = ClimbScoreCalculator.rockTempAdjust(rock[20], 24.0);
        assertThat(adjust).isLessThanOrEqualTo(-8);
        // En régimen normal (roca ≈ aire) el ajuste es 0: el score de siempre.
        assertThat(ClimbScoreCalculator.rockTempAdjust(20.0, 20.0)).isZero();
    }

    @Test
    void laMananaFriaConAireBuenoRecibePremio() {
        assertThat(ClimbScoreCalculator.rockTempAdjust(6.0, 12.0)).isEqualTo(5);
        assertThat(ClimbScoreCalculator.rockTempAdjust(13.0, 17.0)).isEqualTo(3);
        // Roca fría pero aire GÉLIDO: sin premio (escalar a -5° no es "mejor").
        assertThat(ClimbScoreCalculator.rockTempAdjust(-2.0, 2.0)).isZero();
    }
}
