package com.jeremyzay.zaychess.model.pieces;

import java.util.ArrayList;
import java.util.List;

import com.jeremyzay.zaychess.model.board.Board;
import com.jeremyzay.zaychess.model.game.GameState;
import com.jeremyzay.zaychess.model.move.Move;
import com.jeremyzay.zaychess.model.move.MoveType;
import com.jeremyzay.zaychess.model.util.PlayerColor;
import com.jeremyzay.zaychess.model.util.Position;

/**
 * King piece implementation.
 * 
 * Moves one square in any direction.
 * Special castling moves are handled by SpecialMoveHandler.
 */
public class King extends Piece {
    /** Creates a king of the given color at a position. */
    public King(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /**
     * Generates all pseudo-legal king moves (1-square in any direction).
     * Castling is not generated here, but by SpecialMoveHandler.
     */
    @Override
    public List<Move> generatePseudoLegalMoves(GameState gameState, Position from) {
        List<Move> moves = new ArrayList<>();
        Board board = gameState.getBoard();
        addMoves(board, moves, from.getRank(), from.getFile());
        return moves;
    }
    
    /**
     * Attempts one adjacent move.
     * Adds move if valid (empty or capture).
     */
    private Boolean addOneAdjMove(Board board, List<Move> moves, int r, int f, int dirR, int dirF) {
        int rank = r + dirR;
        int file = f + dirF;
        if (!board.isInside(rank, file)) return false;
        
        if ((!board.isEmpty(rank, file) 
             && !getColor().isOpposite(board.getPieceAt(rank, file).getColor()))) return false;
        
        MoveType moveType = board.isEmpty(rank, file) ? MoveType.NORMAL : MoveType.CAPTURE;
        moves.add(new Move(new Position(r,f), new Position(rank, file), moveType));
        return board.isEmpty(rank, file);    
    }
    
    /** Adds all 8 surrounding squares as candidate king moves. */
    private void addMoves(Board board, List<Move> moves, int r, int f) {
        for (int dr : new int[] { -1, 1 }) {
            for (int df : new int[] { -1, 1 }) {
                addOneAdjMove(board, moves, r, f, dr, df);
            }
        }
        for (int dr : new int[] { -1, 1 }) {
            addOneAdjMove(board, moves, r, f, dr, 0);
        }
        for (int df : new int[] { -1, 1 }) {
            addOneAdjMove(board, moves, r, f, 0, df);
        }
    }

    /** @return 'K' symbol for king in FEN/PGN */
    @Override
    public char getSymbol() { return 'K'; }
    
    /** @return "king" */
    @Override
    public String getName() { return "king"; }

    /** Copy constructor. */
    protected King(King other) { super(other); }
        
    /** @return deep copy of this king */
    @Override
    public Piece copy() { return new King(this); }

    /**
     * Checks if this king is currently in check.
     * Iterates over all enemy pieces and their pseudo-legal moves;
     * if any targets this king’s square, it is considered in check.
     * 
     * NOTE: uses pseudo-legal moves (not filtered for king safety),
     * to avoid infinite recursion between generateLegalMoves and isInCheck.
     *
     * @param gameState current game state
     * @return true if threatened by any opposing piece
     */
    public boolean isInCheck(GameState gameState) {
        List<Piece> enemyPieces = gameState.getPiecesOfColor(getColor().getOpposite());
        for (Piece piece : enemyPieces) {
            List<Move> pieceLegalMoves = piece.generatePseudoLegalMoves(gameState, piece.getPos());
            for (Move move : pieceLegalMoves) {
                if (move.getToPos().equals(getPos())) return true; 
            }
        }
        return false;
    }
}
