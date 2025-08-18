package com.frommzay.chess.model.move;

import java.util.List;
import java.util.ArrayList;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.util.Position;

/**
 * Utility class for generating legal moves.
 * 
 * Provides static methods that generate moves for a given piece
 * in the current game state, taking into account castling,
 * en passant, and king safety.
 */
public abstract class MoveGenerator {

    /**
     * Generates all legal moves for the piece at a given position.
     * 
     * Includes pseudo-legal moves from the piece itself, then augments
     * with special moves (castling, en passant), and finally filters out
     * those leaving the king in check.
     *
     * @param s   the game state
     * @param pos the position of the piece to move
     * @return list of fully legal moves
     */
    public static List<Move> generateLegalMoves(GameState s, Position pos) {
        List<Move> pseudoLegalMoves = s.getPieceAt(pos).generatePseudoLegalMoves(s, pos);
        List<Move> legalMoves = new ArrayList<>();
        SpecialMoveHandler smHandler = s.getSpecialMoveHandler();

        smHandler.addEnPassantMoves(s, pos, pseudoLegalMoves);
        smHandler.addCastlingMoves(s, pos, pseudoLegalMoves);

        for (Move move : pseudoLegalMoves) {
            if (MoveValidator.isValidMove(s, move)) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    /**
     * Placeholder: generates all legal moves for a piece whose color matches the turn.
     *
     * @param gameState current state
     * @param pos       position of piece
     * @return list of legal moves (currently unimplemented)
     */
    public static List<Move> generateLegalMovesInTurn(GameState gameState, Position pos) {
        return null;
    }

    /**
     * Returns the legal move from one square to another, if it exists.
     * Also enforces turn order.
     *
     * @param gameState the current game state
     * @param from      origin square
     * @param to        destination square
     * @return the valid move object, or null if not legal
     */
    public static Move getValidMoveInTurn(GameState gameState, Position from, Position to) {
        if (!gameState.isTurn(gameState.getPieceColorAt(from))) {
            System.out.println(gameState.getTurn().toString() + " it's not your turn");
            return null;
        }
        for (Move legalMove : MoveGenerator.generateLegalMoves(gameState, from)) {
            if (legalMove.getToPos().equals(to)) {
                return legalMove;
            }
        }
        return null;
    }
}
