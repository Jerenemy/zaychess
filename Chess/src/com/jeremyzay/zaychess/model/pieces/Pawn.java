package com.jeremyzay.zaychess.model.pieces;

import java.util.List;
import java.util.ArrayList;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * Pawn piece implementation.
 * 
 * Supports single and double pushes, diagonal captures,
 * and promotions. En passant is handled by SpecialMoveHandler.
 */
public class Pawn extends Piece {
    /** Creates a pawn of the given color and position. */
    public Pawn(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /** @return forward direction (+1 for black, -1 for white) */
    private int dir() {
        return (getColor() == PlayerColor.WHITE) ? -1 : 1;
    }

    /** @return starting rank of pawn depending on color */
    private int startRank() {
        return (getColor() == PlayerColor.WHITE) ? 6 : 1;
    }

    /**
     * Generates all pseudo-legal pawn moves from the given square.
     * Includes forward pushes (1–2 squares) and diagonal captures.
     * Promotions are marked with MoveType.PROMOTION.
     */
    @Override
    public List<Move> generatePseudoLegalMoves(GameState gameState, Position from) {
        List<Move> moves = new ArrayList<>();
        int r = from.getRank();
        int f = from.getFile();
        int dir = dir();

        Board board = gameState.getBoard();
        addForwardMoves(board, moves, r, f, dir);
        addCaptureMoves(board, moves, r, f, dir);
        return moves;
    }

    /** Checks if rank is promotion rank for this pawn's color. */
    private boolean isPromotionRank(int rank) {
        return (getColor() == PlayerColor.WHITE) ? (rank == 0) : (rank == 7);
    }

    /** Adds forward one- and two-step moves if path is empty. */
    private void addForwardMoves(Board board, List<Move> moves, int r, int f, int dir) {
        int oneStepRank = r + dir;
        if (!board.isInside(oneStepRank, f)) return;

        if (board.isEmpty(oneStepRank, f)) {
            Position from = new Position(r, f);
            Position to   = new Position(oneStepRank, f);

            if (isPromotionRank(oneStepRank)) {
                moves.add(new Move(from, to, MoveType.PROMOTION));
            } else {
                moves.add(new Move(from, to, MoveType.NORMAL));
                if (r == startRank()) {
                    int twoStepRank = r + 2 * dir;
                    if (board.isInside(twoStepRank, f) && board.isEmpty(twoStepRank, f)) {
                        moves.add(new Move(from, new Position(twoStepRank, f), MoveType.NORMAL));
                    }
                }
            }
        }
    }

    /** Adds diagonal capture moves (promotion if on last rank). */
    private void addCaptureMoves(Board board, List<Move> moves, int r, int f, int dir) {
        for (int df : new int[] { -1, 1 }) {
            int rank = r + dir, file = f + df;
            if (!board.isInside(rank, file)) continue;

            if (!board.isEmpty(rank, file) && getColor().isOpposite(board.getPieceAt(rank, file).getColor())) {
                Position from = new Position(r, f);
                Position to   = new Position(rank, file);

                if (isPromotionRank(rank)) {
                    moves.add(new Move(from, to, MoveType.PROMOTION));
                } else {
                    moves.add(new Move(from, to, MoveType.CAPTURE));
                }
            }
        }
    }

    /** @return symbol 'p' used in FEN/PGN */
    @Override
    public char getSymbol() {
        return 'p';
    }

    /** @return lowercase piece name "pawn" */
    @Override
    public String getName() {
        return "pawn";
    }
    
    /** Copy constructor for Pawn. */
    protected Pawn(Pawn other) {
        super(other);
    }
        
    /** @return deep copy of this pawn */
    @Override
    public Piece copy() {
        return new Pawn(this);
    }
}
