package com.meteomontana.api.application.contribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Parseo del `bloquesJson` de una contribución: ÚNICA fuente de cómo se lee una
 * vía del payload (name/grade/startType/linePath/cara/descripción/variante).
 * Antes este bloque estaba copiado 5 veces dentro de ReviewContributionUseCase
 * y cada copia divergía un poco (así se perdieron description/variant en la
 * rama "corregir vía", bug del 2026-07-18).
 */
public final class ContributionLineParser {

    private ContributionLineParser() {}

    /** Una vía del payload ya parseada y normalizada. */
    public record ParsedLine(
            /** lineId/targetLineId del payload: != null ⇒ corrige una vía existente. */
            String targetLineId,
            /** Nombre tal cual (puede ser ""); el caller decide el default numérico. */
            String name,
            String grade,
            BlockLine.StartType startType,
            String linePath,
            /** Foto de la CARA de la vía: su photoUrl o la portada del bloque. */
            String facePhoto,
            String description,
            String variant
    ) {}

    /**
     * Parsea el array de vías. JSON inválido o no-array → lista vacía (la
     * aprobación nunca revienta por un payload malo; la piedra se crea sin vías).
     */
    public static List<ParsedLine> parse(String bloquesJson, String coverPhoto) {
        List<ParsedLine> out = new ArrayList<>();
        if (bloquesJson == null || bloquesJson.isBlank()) return out;
        try {
            JsonNode arr = new ObjectMapper().readTree(bloquesJson);
            if (!arr.isArray()) return out;
            for (JsonNode node : arr) {
                String targetLineId = textOrNull(node, "lineId");
                if (targetLineId == null) targetLineId = textOrNull(node, "targetLineId");
                String name = node.path("name").asText("").trim();
                String grade = node.path("grade").isNull() ? null
                        : node.path("grade").asText("").trim();
                if (grade != null && grade.isEmpty()) grade = null;
                // App envía PIE/SIT/SEMI/LANCE/TRAV; BD acepta STAND/SIT/SEMI/JUMP/TRAV.
                String rawStart = node.path("startType").isNull() ? null
                        : node.path("startType").asText("").trim();
                out.add(new ParsedLine(
                        targetLineId, name, grade, mapStartType(rawStart),
                        node.path("linePath").asText(null),
                        facePhotoOf(node, coverPhoto),
                        descOf(node), variantOf(node)
                ));
            }
        } catch (Exception ignored) {
            out.clear();
        }
        return out;
    }

    /** Foto (cara) de una vía del JSON: su `photoUrl`, o la portada del bloque. */
    private static String facePhotoOf(JsonNode node, String coverPhoto) {
        String p = node.path("photoUrl").isNull() ? null : node.path("photoUrl").asText(null);
        if (p != null && !p.isBlank()) return p;
        return coverPhoto;
    }

    /** Variante opcional de la vía en el payload (null si vacía). */
    private static String variantOf(JsonNode node) {
        JsonNode v = node.path("variant");
        if (v.isMissingNode() || v.isNull()) return null;
        String t = v.asText("").trim();
        if (t.isEmpty()) return null;
        return t.length() > 60 ? t.substring(0, 60) : t;
    }

    /** Descripción opcional de la vía en el payload (null si vacía). */
    private static String descOf(JsonNode node) {
        JsonNode d = node.path("description");
        if (d.isMissingNode() || d.isNull()) return null;
        String t = d.asText("").trim();
        if (t.isEmpty()) return null;
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String s = n.asText(null);
        return (s == null || s.isBlank()) ? null : s;
    }

    public static BlockLine.StartType mapStartType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.toUpperCase()) {
            case "PIE", "STAND" -> BlockLine.StartType.STAND;
            case "SIT"          -> BlockLine.StartType.SIT;
            case "SEMI"         -> BlockLine.StartType.SEMI;
            case "LANCE", "JUMP" -> BlockLine.StartType.JUMP;
            case "TRAV"         -> BlockLine.StartType.TRAV;
            default             -> null;
        };
    }

    /** Modalidad de la piedra propuesta; default BOULDER si null/desconocida. */
    public static SchoolBlock.Discipline parseDiscipline(String raw) {
        if (raw == null) return SchoolBlock.Discipline.BOULDER;
        try { return SchoolBlock.Discipline.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Discipline.BOULDER; }
    }

    /** Geometría de la piedra propuesta; default POINT si null/desconocida. */
    public static SchoolBlock.Geometry parseGeometry(String raw) {
        if (raw == null) return SchoolBlock.Geometry.POINT;
        try { return SchoolBlock.Geometry.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return SchoolBlock.Geometry.POINT; }
    }
}
