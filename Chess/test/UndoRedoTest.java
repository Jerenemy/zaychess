package com.frommzay.chess.test;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;

import com.frommzay.chess.controller.game.GameController;
import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.pieces.Piece;
import com.frommzay.chess.model.util.Position;

public class UndoRedoTest {
	/**
	 * Prüft, dass ein Bauernzug (e2 -> e4) per Undo zurückgenommen und per Redo
	 * erneut angewendet werden kann.
	 */
	@Test
	public void undoRedoPawn() {
		GameState gs = new GameState();
		GameController gc = new GameController(gs);

		Position e2 = new Position(6, 4);
		Position e4 = new Position(4, 4);

		gc.selectOrMovePiece(e2);
		gc.selectOrMovePiece(e4);

		Piece afterMove = gs.getPieceAt(e4);
		assertNotNull(afterMove);
		assertNull(gs.getPieceAt(e2));

		gc.undo();
		assertNotNull(gs.getPieceAt(e2));
		assertNull(gs.getPieceAt(e4));

		gc.redo();
		assertNotNull(gs.getPieceAt(e4));
		assertNull(gs.getPieceAt(e2));
	}

	/**
	 * Prüft, dass der Redo-Stack geleert wird, wenn nach einem Undo ein anderer Zug
	 * ausgeführt wird.
	 */
	@Test
	public void redoClearAfterUndoTest() {
		GameState gs = new GameState();
		GameController gc = new GameController(gs);

		Position e2 = new Position(6, 4);
		Position e3 = new Position(5, 4);
		Position e4 = new Position(4, 4);

		gc.selectOrMovePiece(e2);
		gc.selectOrMovePiece(e4);
		assertNotNull(gs.getPieceAt(e4));
		assertNull(gs.getPieceAt(e2));

		gc.undo();
		assertNotNull(gs.getPieceAt(e2));
		assertNull(gs.getPieceAt(e4));

		gc.selectOrMovePiece(e2);
		gc.selectOrMovePiece(e3);
		assertNotNull(gs.getPieceAt(e3));
		assertNull(gs.getPieceAt(e2));

		gc.redo();
		assertNotNull(gs.getPieceAt(e3));
		assertNull(gs.getPieceAt(e4));
	}
}
