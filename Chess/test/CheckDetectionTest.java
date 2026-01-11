package com.frommzay.chess.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;

public class CheckDetectionTest {

	private void clearBoard(GameState gs) {
		for (int r = 0; r < 8; r++) {
			for (int f = 0; f < 8; f++) {
				gs.setPieceAt(new Position(r, f), null);
			}
		}
	}

	/**
	 * Prüft, dass die Startposition nicht im Schach ist.
	 */
	@Test
	void startingPositionNoCheck() {
		GameState gs = new GameState();
		assertFalse(gs.isInCheck(), "Startposition sollte kein Schach sein.");
	}

	/**
	 * Prüft, ob ein schwarzer König im Schach erkannt wird, wenn ein weißer Turm
	 * ihn ungehindert angreift.
	 */
	@Test
	void simpleRookCheck() {
		GameState gs = new GameState();
		clearBoard(gs);

		Position blackKingPos = new Position(0, 4);
		Position whiteRookPos = new Position(0, 0);
		gs.setPieceAt(blackKingPos, new King(PlayerColor.BLACK, blackKingPos));
		gs.setPieceAt(whiteRookPos, new Rook(PlayerColor.WHITE, whiteRookPos));

		if (gs.getTurn() != PlayerColor.BLACK) {
			gs.changeTurn();
		}
		assertTrue(gs.isInCheck(), "Das sollte Schach sein.");
	}

	/**
	 * Prüft, dass ein Turm kein Schach gibt, wenn eine Figur dazwischen steht.
	 */
	@Test
	void blockedLineNoCheck() {
		GameState gs = new GameState();
		clearBoard(gs);

		Position blackKingPos = new Position(0, 4);
		Position whiteRookPos = new Position(0, 0);
		Position blockerPos = new Position(0, 2);

		gs.setPieceAt(blackKingPos, new King(PlayerColor.BLACK, blackKingPos));
		gs.setPieceAt(whiteRookPos, new Rook(PlayerColor.WHITE, whiteRookPos));
		gs.setPieceAt(blockerPos, new Knight(PlayerColor.WHITE, blockerPos));

		if (gs.getTurn() != PlayerColor.BLACK) {
			gs.changeTurn();
		}
		assertFalse(gs.isInCheck(), "Falsches Schach.");
	}
}
