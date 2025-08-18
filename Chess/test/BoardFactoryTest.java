package com.frommzay.chess.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.frommzay.chess.model.board.Board;
import com.frommzay.chess.model.board.BoardFactory;

public class BoardFactoryTest {

	/**
	 * Prüft, ob die Figuren in der Anfangsaufstellung auf den hinteren Rängen in
	 * der richtigen Reihenfolge stehen.
	 */
	@Test
	void defaultBoardPiecesTest() {
		Board b = BoardFactory.createDefaultBoard();
		// black rank
		assertEquals('R', b.getPieceAt(0, 0).getSymbol());
		assertEquals('H', b.getPieceAt(0, 1).getSymbol());
		assertEquals('B', b.getPieceAt(0, 2).getSymbol());
		assertEquals('Q', b.getPieceAt(0, 3).getSymbol());
		assertEquals('K', b.getPieceAt(0, 4).getSymbol());
		assertEquals('B', b.getPieceAt(0, 5).getSymbol());
		assertEquals('H', b.getPieceAt(0, 6).getSymbol());
		assertEquals('R', b.getPieceAt(0, 7).getSymbol());

		// white rank
		assertEquals('R', b.getPieceAt(7, 0).getSymbol());
		assertEquals('H', b.getPieceAt(7, 1).getSymbol());
		assertEquals('B', b.getPieceAt(7, 2).getSymbol());
		assertEquals('Q', b.getPieceAt(7, 3).getSymbol());
		assertEquals('K', b.getPieceAt(7, 4).getSymbol());
		assertEquals('B', b.getPieceAt(7, 5).getSymbol());
		assertEquals('H', b.getPieceAt(7, 6).getSymbol());
		assertEquals('R', b.getPieceAt(7, 7).getSymbol());
	}

	/**
	 * Prüft, dass das Zentrum des Spielbrettes in der Anfangsstellung leer ist.
	 */
	@Test
	void defaultBoardVoidTest() {
		Board b = BoardFactory.createDefaultBoard();
		assertNull(b.getPieceAt(4, 4));
		assertNull(b.getPieceAt(3, 3));
		assertNull(b.getPieceAt(4, 3));
	}
}
