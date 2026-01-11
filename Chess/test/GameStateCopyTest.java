package com.frommzay.chess.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;
import com.frommzay.chess.model.move.Move;
import com.frommzay.chess.model.move.MoveType;

public class GameStateCopyTest {

	/**
	 * Prüft, ob Änderungen im kopierten Spielzustand das Original beeinflussen.
	 */
	@Test
	void copy_isDeepIndependentBoard() {
		GameState original = new GameState();
		Position p = new Position(4, 4);
		Rook rook = new Rook(PlayerColor.WHITE, p);
		original.setPieceAt(p, rook);

		GameState copy = original.copy();
		Position q = new Position(4, 5);
		Move m = new Move(p, q, MoveType.NORMAL);
		copy.applyMove(m);

		assertNotNull(original.getPieceAt(p));
		assertNull(copy.getPieceAt(p));
	}
}
