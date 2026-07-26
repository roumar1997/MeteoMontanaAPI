package com.meteomontana.api.application.contribution;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.PendingContribution;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.infrastructure.persistence.jpa.BlockLineJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SchoolBlockJpaEntity;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataJournalRepository;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataSchoolBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reconciliación de vías al aprobar contribuciones. Zona del bug de homónimas
 * ("La ola"): lo crítico es que el id de una vía existente SOBREVIVE al editar
 * (los enganches del diario por lineId siguen válidos), que las omitidas se
 * borran, y que descripción/variante se aplican (bug 2026-07-18).
 */
class LineReconcilerTest {

    SpringDataSchoolBlockRepository blockRepo = mock(SpringDataSchoolBlockRepository.class);
    SpringDataJournalRepository journalRepo   = mock(SpringDataJournalRepository.class);
    LineReconciler reconciler;

    @BeforeEach void setUp() {
        reconciler = new LineReconciler(blockRepo, journalRepo);
        when(blockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SchoolBlockJpaEntity blockWith(BlockLineJpaEntity... lines) {
        var block = new SchoolBlockJpaEntity("B1", "school-1", SchoolBlock.Type.BLOCK, "Piedra",
                40.0, -3.0, "cover.jpg", null, "uid", LocalDateTime.now());
        for (var l : lines) block.addLine(l);
        when(blockRepo.findById("B1")).thenReturn(Optional.of(block));
        return block;
    }

    private BlockLineJpaEntity line(String id, String name, String grade) {
        return new BlockLineJpaEntity(id, name, grade, BlockLine.StartType.STAND, "[[0,0]]", 0, "cover.jpg", 0);
    }

    private PendingContribution contribution(String bloquesJson) {
        PendingContribution c = mock(PendingContribution.class);
        when(c.getTargetBlockId()).thenReturn("B1");
        when(c.getBloquesJson()).thenReturn(bloquesJson);
        when(c.getGeometry()).thenReturn("LINE");
        when(c.getPath()).thenReturn("[[0,0],[1,1]]");
        when(c.getDirection()).thenReturn("LTR");
        return c;
    }

    @Test void reconcileWall_preserva_id_de_via_existente_al_editar() {
        var block = blockWith(line("keep-id", "La ola", "7a"));
        // Payload edita esa misma vía (targetLineId=keep-id) con grado nuevo.
        var c = contribution("[{\"targetLineId\":\"keep-id\",\"name\":\"La ola\",\"grade\":\"7b\"}]");
        reconciler.reconcileWall(c);

        assertThat(block.getLines()).hasSize(1);
        assertThat(block.getLines().get(0).getId()).isEqualTo("keep-id");   // id SOBREVIVE
        verify(journalRepo).updateGradeByLineId("keep-id", "7b");           // grado propagado
    }

    @Test void reconcileWall_crea_nuevas_y_borra_las_omitidas() {
        var block = blockWith(line("old-1", "Vieja", "6a"), line("old-2", "Otra", "6b"));
        // El payload solo trae old-1 (se conserva) + una vía NUEVA (sin id). old-2 se omite → se borra.
        var c = contribution("[{\"targetLineId\":\"old-1\",\"name\":\"Vieja\",\"grade\":\"6a\"}," +
                "{\"name\":\"Nueva\",\"grade\":\"7c\"}]");
        var firstCreated = reconciler.reconcileWall(c);

        assertThat(block.getLines()).extracting(BlockLineJpaEntity::getId).contains("old-1");
        assertThat(block.getLines()).extracting(BlockLineJpaEntity::getId).doesNotContain("old-2");
        assertThat(block.getLines()).hasSize(2);
        assertThat(firstCreated).isNotNull();
        assertThat(firstCreated.getName()).isEqualTo("Nueva");
    }

    @Test void updateExistingLine_aplica_descripcion_y_variante() {
        var existing = line("L1", "La ola", "7a");
        blockWith(existing);
        var c = contribution("[{\"targetLineId\":\"L1\",\"name\":\"La ola\",\"grade\":\"7a\"," +
                "\"description\":\"nueva desc\",\"variant\":\"directa\"}]");
        when(c.getTargetLineId()).thenReturn("L1");

        reconciler.updateExistingLine(c);

        // El bug 2026-07-18: description/variant NO se aplicaban. Ahora sí.
        assertThat(existing.getDescription()).isEqualTo("nueva desc");
        assertThat(existing.getVariant()).isEqualTo("directa");
        assertThat(existing.getId()).isEqualTo("L1");   // sigue siendo la misma vía
    }
}
