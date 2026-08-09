package com.meteomontana.api.application.blocks;

import com.meteomontana.api.application.blocks.BlockLineIdReconciler.Incoming;
import com.meteomontana.api.domain.model.BlockLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Del id de una vía cuelgan el ✓ del diario, las estrellas, los votos de grado,
 * los comentarios, los posts del feed y los enlaces compartidos. Estas pruebas
 * fijan que editar una piedra no se lo lleva por delante.
 */
class BlockLineIdReconcilerTest {

    private static BlockLine via(String id, String name, int face) {
        return new BlockLine(id, "bloque-1", name, "6a", null, null, 0, null, face);
    }

    @Test void mismasViasMismosIds() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola", 0), new Incoming("Los perros", 0)));
        assertThat(ids).containsExactly("id-A", "id-B");
    }

    @Test void cambiarElGradoNoTocaLosIds() {
        // El editor manda nombre + cara; el grado va aparte. Si el nombre no
        // cambia, la vía es la misma.
        var existentes = List.of(via("id-A", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes, List.of(new Incoming("La ola", 0)));
        assertThat(ids).containsExactly("id-A");
    }

    @Test void renombrarUnaViaConservaSuIdPorPosicion() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola grande", 0), new Incoming("Los perros", 0)));
        // "Los perros" casa por nombre; la renombrada hereda la que queda libre.
        assertThat(ids).containsExactly("id-A", "id-B");
    }

    @Test void unaViaNuevaEstrenaId() {
        var existentes = List.of(via("id-A", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola", 0), new Incoming("Vía nueva", 0)));
        assertThat(ids.get(0)).isEqualTo("id-A");
        assertThat(ids.get(1)).isNotNull().isNotEqualTo("id-A");
    }

    @Test void borrarLaPrimeraNoDesplazaElIdDeLaSegunda() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes, List.of(new Incoming("Los perros", 0)));
        assertThat(ids).containsExactly("id-B");
    }

    @Test void reordenarLasViasNoIntercambiaSusIds() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("Los perros", 0), new Incoming("La ola", 0)));
        assertThat(ids).containsExactly("id-B", "id-A");
    }

    @Test void cadaCaraSeEmparejaConLaSuya() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "La ola", 1));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola", 1), new Incoming("La ola", 0)));
        assertThat(ids).containsExactly("id-B", "id-A");
    }

    @Test void dosViasHomonimasEnLaMismaCaraNoComparteIdd() {
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola", 0), new Incoming("La ola", 0)));
        assertThat(ids).containsExactly("id-A", "id-B");
    }

    @Test void unaViaSinNombreSeEmparejaPorPosicion() {
        var existentes = List.of(via("id-A", "", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("", 0), new Incoming("Los perros", 0)));
        assertThat(ids).containsExactly("id-A", "id-B");
    }

    @Test void unNombreConBarraNoSeConfundeConOtraCara() {
        // La clave de emparejamiento es cara+nombre. Si se compusiera
        // concatenando con un separador, "2|La ola" de la cara 1 chocaría con
        // "La ola" de la cara 12 y se intercambiarían los ids.
        var existentes = List.of(via("id-A", "2|La ola", 1), via("id-B", "La ola", 12));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("La ola", 12), new Incoming("2|La ola", 1)));
        assertThat(ids).containsExactly("id-B", "id-A");
    }

    // ── Cuando el editor manda el id (apps 2.21.3+) ─────────────────────

    @Test void elIdQueMandaElEditorManda() {
        // Dos vias homonimas en la misma cara: por nombre no hay forma de
        // saber cual es cual, y ese era justo el caso que podia mover el ✓ del
        // diario de alguien a otra via.
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("id-B", "La ola", 0), new Incoming("id-A", "La ola", 0)));
        assertThat(ids).containsExactly("id-B", "id-A");
    }

    @Test void conIdSePuedeRenombrarSinPerderLaVia() {
        var existentes = List.of(via("id-A", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("id-A", "Nombre nuevo del todo", 0)));
        assertThat(ids).containsExactly("id-A");
    }

    @Test void borrarUnaYAnadirOtraALaVezYaNoConfunde() {
        // El caso que el emparejamiento por posicion podia fallar: se borra la
        // primera y se anade una nueva en el mismo guardado.
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("id-B", "Los perros", 0), new Incoming(null, "Via nueva", 0)));
        assertThat(ids.get(0)).isEqualTo("id-B");
        assertThat(ids.get(1)).isNotIn("id-A", "id-B");
    }

    @Test void unIdDesconocidoNoRompeNada() {
        // Una via que ya no existe (la borro otro mientras editabas): se trata
        // como nueva en vez de reventar.
        var existentes = List.of(via("id-A", "La ola", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("id-fantasma", "Otra cosa", 0)));
        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isNotEqualTo("id-A");
    }

    @Test void siElPayloadTraeIdsUnaFilaSinIdEsUnaViaNueva() {
        // Un payload viene de UNA app: si trae ids, la fila que no lo trae es
        // nueva de verdad. Adivinarle un id le colgaria el ✓ del diario de
        // alguien a una via recien nacida.
        var existentes = List.of(via("id-A", "La ola", 0), via("id-B", "Los perros", 0));
        var ids = BlockLineIdReconciler.assignIds(existentes,
                List.of(new Incoming("id-B", "Los perros", 0), new Incoming("La ola", 0)));
        assertThat(ids.get(0)).isEqualTo("id-B");
        assertThat(ids.get(1)).isNotIn("id-A", "id-B");
    }

    @Test void sinViasPreviasTodoEsNuevo() {
        var ids = BlockLineIdReconciler.assignIds(List.of(), List.of(new Incoming("La ola", 0)));
        assertThat(ids).hasSize(1);
        assertThat(ids.get(0)).isNotBlank();
    }
}
