package com.meteomontana.api.application.contribution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** F: la orientacion que pone el AUTOR al crear la piedra -> su primer voto. */
class AuthorOrientationsTest {

    @Test
    void parseaPiedraEnteraYCaras() {
        var votes = BlockMaterializer.parseAuthorOrientations(
                "{\"block\":\"NE\",\"faces\":{\"0\":\"N\",\"2\":\"S\"}}", "b1", "uid1");
        assertEquals(3, votes.size());
        assertNull(votes.get(0).photoIndex());
        assertEquals("NE", votes.get(0).aspect());
        assertEquals("uid1", votes.get(0).voterUid());
        assertEquals(0, votes.get(1).photoIndex());
        assertEquals(2, votes.get(2).photoIndex());
    }

    @Test
    void ignoraRumbosInvalidosYJsonRoto() {
        assertTrue(BlockMaterializer.parseAuthorOrientations("{no json", "b", "u").isEmpty());
        assertTrue(BlockMaterializer.parseAuthorOrientations(
                "{\"block\":\"NORTE\"}", "b", "u").isEmpty());
        assertTrue(BlockMaterializer.parseAuthorOrientations(null, "b", "u").isEmpty());
        // sin uid no hay voto que atribuir
        assertTrue(BlockMaterializer.parseAuthorOrientations(
                "{\"block\":\"N\"}", "b", null).isEmpty());
    }
}
