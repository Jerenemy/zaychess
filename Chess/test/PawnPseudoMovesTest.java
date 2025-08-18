package com.frommzay.chess.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.move.Move;
import com.frommzay.chess.model.pieces.Pawn;
import com.frommzay.chess.model.util.Position;

public class PawnPseudoMovesTest {

	/**
	 * Weißer Bauer sollte vom Startfeld sowohl 1 als auch 2 Felder vorwärts ziehen
	 * können, wenn der Weg frei ist.
	 */
	@Test
	void whitePawnFromStart_twoForwardMovesIfUnblocked() {
		GameState gs = new GameState();
		Position e2 = new Position(6, 4);

		gs.setPieceAt(new Position(5, 4), null);
		gs.setPieceAt(new Position(4, 4), null);

		Pawn p = (Pawn) gs.getPieceAt(e2);
		List<Move> moves = p.generatePseudoLegalMoves(gs, e2);

		long forwardTargets = moves.stream().filter(
				m -> m.getToPos().getFile() == 4 && (m.getToPos().getRank() == 5 || m.getToPos().getRank() == 4))
				.count();
		assertEquals(2, forwardTargets, "White pawn movement wrong.");
	}

	/**
	 * Schwarzer Bauer sollte vom Startfeld sowohl 1 als auch 2 Felder vorwärts
	 * ziehen können, wenn der Weg frei ist.
	 */
	@Test
	void blackPawnFromStart_twoForwardMovesIfUnblocked() {
		GameState gs = new GameState();
		Position e7 = new Position(1, 4);

		gs.setPieceAt(new Position(2, 4), null);
		gs.setPieceAt(new Position(3, 4), null);

		Pawn p = (Pawn) gs.getPieceAt(e7);
		List<Move> moves = p.generatePseudoLegalMoves(gs, e7);

		long forwardTargets = moves.stream().filter(
				m -> m.getToPos().getFile() == 4 && (m.getToPos().getRank() == 2 || m.getToPos().getRank() == 3))
				.count();
		assertEquals(2, forwardTargets, "Black pawn movement wrong.");
	}
}
