package com.frommzay.chess.model.move;

import com.frommzay.chess.model.game.GameState;

/**
 * Provides static methods to validate moves against chess rules.
 * 
 * Currently ensures that a move does not leave the player's king in check.
 */
public class MoveValidator {

    /**
     * Checks if a move is legal in principle.
     * 
     * @param state current game state
     * @param move  the move to test
     * @return true if valid, false if it violates king safety
     */
    public static boolean isValidMove(GameState state, Move move) {
        return !leavesKingInCheck(state, move);
    }

    /**
     * Tests whether a move would leave the mover's king in check.
     * 
     * Simulates the move on a copy of the game state and checks the result.
     *
     * @param gameState current game state
     * @param move      the move to test
     * @return true if the king is in check after the move
     */
    public static boolean leavesKingInCheck(GameState gameState, Move move) {
        GameState probe = gameState.copy();
        probe.applyMove(move);     // simulate the move
        probe.changeTurn();        // switch back to mover's perspective
        return probe.isInCheck();
    }
}
