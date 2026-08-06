package com.meteomontana.api.application.blocks;

import com.meteomontana.api.domain.model.BlockLine;
import com.meteomontana.api.domain.model.SchoolBlock;
import com.meteomontana.api.domain.model.User;
import com.meteomontana.api.domain.port.LineRatingRepository;
import com.meteomontana.api.domain.port.SchoolBlockRepository;
import com.meteomontana.api.domain.port.SchoolRepository;
import com.meteomontana.api.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Editar un bloque borra la fila y la reinserta, que es como se limpian las
 * vías viejas. Pero la clave que une una piedra con su sector es
 * {@code ON DELETE SET NULL}: si eso se hace con un SECTOR, sus piedras se
 * quedan sueltas y reinsertar la fila con el mismo id NO las recupera.
 *
 * De ahí la regla: solo se borra lo que tiene vías que reescribir, o sea las
 * piedras y muros — que además nunca son padres de nadie.
 */
class UpdateBlockKeepsSectorTest {

    private SchoolBlockRepository blocks;
    private SchoolBlockUseCase useCase;

    private static SchoolBlock bloque(String id, SchoolBlock.Type type, String name) {
        return new SchoolBlock(id, "la-pedriza", type, name, 40.0, -3.0, null, null,
                "uid-otro", LocalDateTime.now(), List.of(), null);
    }

    @BeforeEach void setUp() {
        blocks = mock(SchoolBlockRepository.class);
        when(blocks.save(any())).thenAnswer(i -> i.getArgument(0));
        UserRepository users = mock(UserRepository.class);
        when(users.findByUid("uid-admin")).thenReturn(Optional.of(
                new User("uid-admin", "a@x.com", "admin", "Admin", null, null, true, null,
                        true, false, null, null, null, null)));
        useCase = new SchoolBlockUseCase(blocks, mock(SchoolRepository.class), users,
                mock(LineRatingRepository.class));
    }

    private SchoolBlockUseCase.CreateBlockRequest renombrar(String nombre) {
        return new SchoolBlockUseCase.CreateBlockRequest(
                null, nombre, null, null, null, null, List.of(), null, null, null, null, null);
    }

    @Test void renombrarUnSectorNoBorraLaFila() {
        when(blocks.findById("zona-1")).thenReturn(
                Optional.of(bloque("zona-1", SchoolBlock.Type.ZONE, "La Grieta")));

        var dto = useCase.update("uid-admin", "zona-1", renombrar("La Raja"));

        assertThat(dto.name()).isEqualTo("La Raja");
        // Lo importante: NO se borra → las piedras del sector conservan su vínculo.
        verify(blocks, never()).deleteById(anyString());
        verify(blocks).save(any());
    }

    @Test void renombrarUnParkingTampocoBorraLaFila() {
        when(blocks.findById("park-1")).thenReturn(
                Optional.of(bloque("park-1", SchoolBlock.Type.PARKING, "Parking de la Grieta")));

        useCase.update("uid-admin", "park-1", renombrar("Parking de la Raja"));

        verify(blocks, never()).deleteById(anyString());
    }

    @Test void editarUnaPiedraTampocoBorraLaFila() {
        when(blocks.findById("piedra-1")).thenReturn(
                Optional.of(bloque("piedra-1", SchoolBlock.Type.BLOCK, "Piedra 7")));

        useCase.update("uid-admin", "piedra-1", renombrar("Piedra siete"));

        // Borrar la fila arrastraba las vías, y con ellas —por cascade— las
        // estrellas y los votos de grado de todo el mundo. Guardar encima ya
        // reconcilia (orphanRemoval quita las vías que el editor omitió).
        verify(blocks, never()).deleteById(anyString());
    }

    @Test void editarUnaPiedraConservaElIdDeSusVias() {
        var piedra = new SchoolBlock("piedra-1", "la-pedriza", SchoolBlock.Type.BLOCK,
                "Piedra 7", 40.0, -3.0, null, null, "uid-otro", LocalDateTime.now(),
                List.of(new BlockLine("via-vieja", "piedra-1", "Los perros", "7a",
                        null, null, 0, null, 0)), null);
        when(blocks.findById("piedra-1")).thenReturn(Optional.of(piedra));

        var req = new SchoolBlockUseCase.CreateBlockRequest(
                null, "Piedra 7", null, null, null, null,
                List.of(new SchoolBlockUseCase.CreateBlockLineRequest(
                        "via-vieja", "Los perros", "7b", null, null, null, 0, null, null)),
                null, null, null, null, null);
        var dto = useCase.update("uid-admin", "piedra-1", req);

        // El grado cambia; el id NO. Si cambiase, el ✓ del diario de quien la
        // tenga hecha, sus estrellas y sus votos se quedarían huérfanos.
        assertThat(dto.lines()).hasSize(1);
        assertThat(dto.lines().get(0).id()).isEqualTo("via-vieja");
        assertThat(dto.lines().get(0).grade()).isEqualTo("7b");
    }
}
