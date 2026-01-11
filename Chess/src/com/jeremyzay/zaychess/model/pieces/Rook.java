package com.jeremyzay.zaychess.model.pieces;

import java.util.ArrayList;
import java.util.List;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;
import com.jeremyzay.zaychess.model.move.MoveValidator;

/**
 * Rook piece implementation.
 *
 * Moves in straight lines (ranks/files) until blocked by
 * the edge of the board, a friendly piece, or an enemy capture.
 */
public class Rook extends Piece {
    /** Creates a rook of the given color at a position. */
    public Rook(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /**
     * Generates all pseudo-legal rook moves (horizontal & vertical).
     * Ignores king-safety checks, which are handled in {@link MoveValidator}.
     *
     * @param gameState current game state
     * @param from rook’s position
     * @return list of moves (NORMAL or CAPTURE)
     */
    @Override
    public List<Move> generatePseudoLegalMoves(GameState gameState, Position from) {
        List<Move> moves = new ArrayList<>();
        Board board = gameState.getBoard();
        addMoves(board, moves, from.getRank(), from.getFile());
        return moves;
    }

    /**
     * Attempts one step in a given direction.
     * Adds move if valid, and indicates whether movement can continue.
     *
     * @return true if square was empty, false if blocked
     */
    private Boolean addOneAdjMove(Board board, List<Move> moves,
                                  int orig_r, int orig_f, int r_i, int f_i,
                                  int dirR, int dirF) {
        int rank = r_i + dirR;
        int file = f_i + dirF;
        if (!board.isInside(rank, file)) return false;

        if (!board.isEmpty(rank, file) && !getColor().isOpposite(board.getPieceAt(rank, file).getColor())) return false;

        MoveType moveType = board.isEmpty(rank, file) ? MoveType.NORMAL : MoveType.CAPTURE;
        moves.add(new Move(new Position(orig_r, orig_f), new Position(rank, file), moveType));

        return board.isEmpty(rank, file);
    }

    /** Expands rook moves along ranks and files. */
    private void addMoves(Board board, List<Move> moves, int r, int f) {
        for (int dr : new int[] { -1, 1 }) {
            int r_i = r;
            while (addOneAdjMove(board, moves, r, f, r_i, f, dr, 0)) r_i += dr;
        }
        for (int df : new int[] { -1, 1 }) {
            int f_i = f;
            while (addOneAdjMove(board, moves, r, f, r, f_i, 0, df)) f_i += df;
        }
    }

    /** @return 'R' symbol */
    @Override
    public char getSymbol() { return 'R'; }

    /** @return "rook" */
    @Override
    public String getName() { return "rook"; }

    /** Copy constructor. */
    protected Rook(Rook other) { super(other); }

    /** @return deep copy of this rook */
    @Override
    public Rook copy() { return new Rook(this); }
}
