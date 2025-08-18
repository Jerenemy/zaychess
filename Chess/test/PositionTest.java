package com.frommzay.chess.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.frommzay.chess.model.util.Position;

public class PositionTest {

	/**
	 * Prüft, ob zwei Positionen mit gleichen Koordinaten als gleich gelten.
	 */
	@Test
	void equalsSameCoords() {
		Position a = new Position(6, 4);
		Position b = new Position(6, 4);
		assertEquals(a, b);
		assertEquals(a.toString(), b.toString());
	}

	/**
	 * Prüft, ob eine IllegalArgumentException geworfen wird, wenn Koordinaten
	 * außerhalb des gültigen Bereichs liegen.
	 */
	@Test
	void outOfRangeThrows() {
		assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
		assertThrows(IllegalArgumentException.class, () -> new Position(8, 0));
		assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
		assertThrows(IllegalArgumentException.class, () -> new Position(0, 8));
	}
}
