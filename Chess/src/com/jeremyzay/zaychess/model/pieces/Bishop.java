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
 * Bishop piece implementation.
 *
 * Moves diagonally in all directions until blocked by
 * the edge of the board, a friendly piece, or an enemy capture.
 */
public class Bishop extends Piece {
    /** Creates a bishop of the given color at a position. */
    public Bishop(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /**
     * Generates all pseudo-legal bishop moves (diagonals).
     * Ignores king-safety checks, which are handled in {@link MoveValidator}.
     *
     * @param gameState current game state
     * @param from bishop’s position
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
     * Attempts one step in a diagonal direction.
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

    /** Expands bishop moves along all four diagonals. */
    private void addMoves(Board board, List<Move> moves, int r, int f) {
        for (int dr : new int[] { -1, 1 }) {
            for (int df : new int[] { -1, 1 }) {
                int r_i = r, f_i = f;
                while (addOneAdjMove(board, moves, r, f, r_i, f_i, dr, df)) {
                    r_i += dr;
                    f_i += df;
                }
            }
        }
    }

    /** @return 'B' symbol */
    @Override
    public char getSymbol() { return 'B'; }

    /** @return "bishop" */
    @Override
    public String getName() { return "bishop"; }

    /** Copy constructor. */
    protected Bishop(Bishop other) { super(other); }

    /** @return deep copy of this bishop */
    @Override
    public Piece copy() { return new Bishop(this); }
}
