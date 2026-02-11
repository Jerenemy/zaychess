package com.jeremyzay.zaychess.model.move;

import java.util.List;
import java.util.ArrayList;

import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.util.Position;

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
     * Generates all legal moves for the player whose turn it is.
     *
     * @param gameState current state
     * @return list of all fully legal moves for the active color
     */
    public static List<Move> generateAllLegalMovesInTurn(GameState gameState) {
        List<Move> allMoves = new ArrayList<>();
        List<com.jeremyzay.zaychess.model.pieces.Piece> pieces = gameState.getPiecesOfColor(gameState.getTurn());
        for (com.jeremyzay.zaychess.model.pieces.Piece piece : pieces) {
            allMoves.addAll(generateLegalMoves(gameState, piece.getPos()));
        }
        return allMoves;
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
        if (gameState.getPieceAt(from) == null) {
            return null;
        }
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
