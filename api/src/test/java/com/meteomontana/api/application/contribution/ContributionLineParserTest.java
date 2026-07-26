package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parser del `bloquesJson` de una contribución. Cubre el bug del 2026-07-18
 * (se perdían description/variant en "corregir vía") y el mapeo de startType.
 */
class ContributionLineParserTest {

    @Test void parsea_via_completa_con_descripcion_y_variante() {
        String json = "[{\"lineId\":\"L1\",\"name\":\"La ola\",\"grade\":\"7a\"," +
                "\"startType\":\"PIE\",\"linePath\":\"[[0,0]]\",\"photoUrl\":\"face.jpg\"," +
                "\"description\":\"chulísima\",\"variant\":\"directa\"}]";
        List<ContributionLineParser.ParsedLine> out = ContributionLineParser.parse(json, "cover.jpg");
        assertThat(out).hasSize(1);
        var l = out.get(0);
        assertThat(l.targetLineId()).isEqualTo("L1");
        assertThat(l.name()).isEqualTo("La ola");
        assertThat(l.grade()).isEqualTo("7a");
        assertThat(l.startType()).isEqualTo(BlockLine.StartType.STAND);   // PIE → STAND
        assertThat(l.facePhoto()).isEqualTo("face.jpg");
        assertThat(l.description()).isEqualTo("chulísima");               // el bug: antes se perdía
        assertThat(l.variant()).isEqualTo("directa");                     // el bug: antes se perdía
    }

    @Test void targetLineId_como_alternativa_a_lineId() {
        var out = ContributionLineParser.parse("[{\"targetLineId\":\"T9\",\"name\":\"x\"}]", null);
        assertThat(out.get(0).targetLineId()).isEqualTo("T9");
    }

    @Test void sin_photoUrl_usa_la_portada_del_bloque() {
        var out = ContributionLineParser.parse("[{\"name\":\"x\"}]", "cover.jpg");
        assertThat(out.get(0).facePhoto()).isEqualTo("cover.jpg");
    }

    @Test void grade_vacio_y_desc_variante_vacias_son_null() {
        var out = ContributionLineParser.parse(
                "[{\"name\":\"x\",\"grade\":\"\",\"description\":\"  \",\"variant\":\"\"}]", null);
        assertThat(out.get(0).grade()).isNull();
        assertThat(out.get(0).description()).isNull();
        assertThat(out.get(0).variant()).isNull();
    }

    @Test void json_invalido_devuelve_lista_vacia() {
        assertThat(ContributionLineParser.parse("no soy json", null)).isEmpty();
    }

    @Test void json_no_array_devuelve_lista_vacia() {
        assertThat(ContributionLineParser.parse("{\"name\":\"x\"}", null)).isEmpty();
    }

    @Test void json_nulo_o_vacio_devuelve_lista_vacia() {
        assertThat(ContributionLineParser.parse(null, null)).isEmpty();
        assertThat(ContributionLineParser.parse("   ", null)).isEmpty();
    }

    @Test void mapeo_de_todos_los_startType() {
        assertThat(ContributionLineParser.mapStartType("PIE")).isEqualTo(BlockLine.StartType.STAND);
        assertThat(ContributionLineParser.mapStartType("STAND")).isEqualTo(BlockLine.StartType.STAND);
        assertThat(ContributionLineParser.mapStartType("SIT")).isEqualTo(BlockLine.StartType.SIT);
        assertThat(ContributionLineParser.mapStartType("SEMI")).isEqualTo(BlockLine.StartType.SEMI);
        assertThat(ContributionLineParser.mapStartType("LANCE")).isEqualTo(BlockLine.StartType.JUMP);
        assertThat(ContributionLineParser.mapStartType("JUMP")).isEqualTo(BlockLine.StartType.JUMP);
        assertThat(ContributionLineParser.mapStartType("TRAV")).isEqualTo(BlockLine.StartType.TRAV);
        assertThat(ContributionLineParser.mapStartType("desconocido")).isNull();
        assertThat(ContributionLineParser.mapStartType(null)).isNull();
    }

    @Test void descripcion_y_variante_se_truncan() {
        String longDesc = "d".repeat(600);
        String longVar = "v".repeat(80);
        var out = ContributionLineParser.parse(
                "[{\"name\":\"x\",\"description\":\"" + longDesc + "\",\"variant\":\"" + longVar + "\"}]", null);
        assertThat(out.get(0).description()).hasSize(500);
        assertThat(out.get(0).variant()).hasSize(60);
    }

    @Test void defaults_de_disciplina_y_geometria() {
        assertThat(ContributionLineParser.parseDiscipline(null)).isEqualTo(SchoolBlock.Discipline.BOULDER);
        assertThat(ContributionLineParser.parseDiscipline("raro")).isEqualTo(SchoolBlock.Discipline.BOULDER);
        assertThat(ContributionLineParser.parseGeometry(null)).isEqualTo(SchoolBlock.Geometry.POINT);
        assertThat(ContributionLineParser.parseGeometry("raro")).isEqualTo(SchoolBlock.Geometry.POINT);
    }
}
