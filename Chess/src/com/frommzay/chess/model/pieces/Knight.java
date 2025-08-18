package com.frommzay.chess.model.pieces;

import java.util.ArrayList;
import java.util.List;

import com.frommzay.chess.model.board.Board;
import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.move.Move;
import com.frommzay.chess.model.move.MoveType;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;

/**
 * Knight piece implementation.
 *
 * Moves in an L-shape: two squares in one direction and
 * one square perpendicular. Can jump over other pieces.
 */
public class Knight extends Piece {
    /** Creates a knight of the given color at a position. */
    public Knight(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /**
     * Generates all pseudo-legal knight moves (L-shapes).
     * Ignores king-safety checks, which are handled in {@link com.frommzay.chess.model.move.MoveValidator}.
     *
     * @param gameState current game state
     * @param from knight’s position
     * @return list of moves (NORMAL or CAPTURE)
     */
    @Override
    public List<Move> generatePseudoLegalMoves(GameState gameState, Position from) {
        List<Move> moves = new ArrayList<>();
        Board board = gameState.getBoard();
        addMoves(board, moves, from.getRank(), from.getFile());
        return moves;
    }

    /** Adds a knight move in a single (dr, df) offset if valid. */
    private void addOneMove(Board board, List<Move> moves, int r, int f, int dirR, int dirF) {
        int rank = r + dirR;
        int file = f + dirF;
        if (!board.isInside(rank, file)) return;

        if (!board.isEmpty(rank, file) && !getColor().isOpposite(board.getPieceAt(rank, file).getColor())) return;

        MoveType moveType = board.isEmpty(rank, file) ? MoveType.NORMAL : MoveType.CAPTURE;
        moves.add(new Move(new Position(r, f), new Position(rank, file), moveType));
    }

    /** Adds all 8 knight L-shaped moves. */
    private void addMoves(Board board, List<Move> moves, int r, int f) {
        int[] deltas = { 2, 1, -1, -2 };
        for (int dr : deltas) {
            for (int df : deltas) {
                if (Math.abs(dr) != Math.abs(df)) {
                    addOneMove(board, moves, r, f, dr, df);
                }
            }
        }
    }

    /** @return 'N' symbol */
    @Override
    public char getSymbol() { return 'N'; }

    /** @return "knight" */
    @Override
    public String getName() { return "knight"; }

    /** Copy constructor. */
    protected Knight(Knight other) { super(other); }

    /** @return deep copy of this knight */
    @Override
    public Knight copy() { return new Knight(this); }
}
