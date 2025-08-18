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
 * Queen piece implementation.
 * 
 * Combines the movement of rook and bishop:
 * - unlimited straight lines (ranks/files)
 * - unlimited diagonals
 * Stops when reaching the board edge, a friendly piece, 
 * or after capturing an enemy piece.
 */
public class Queen extends Piece {
    /** Creates a queen of the given color at a position. */
    public Queen(PlayerColor color, Position pos) {
        super(color, pos);
    }

    /**
     * Generates all pseudo-legal queen moves:
     * horizontal, vertical, and diagonal rays until blocked.
     * 
     * @param gameState current game state
     * @param from      queen's position
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
     * Attempts one step in a direction.
     * Adds move if valid, and returns whether movement can continue further.
     * 
     * @return true if square was empty (continue ray), false if blocked
     */
    private Boolean addOneAdjMove(Board board, List<Move> moves, int orig_r, int orig_f,
                                  int r_i, int f_i, int dirR, int dirF) {
        int rank = r_i + dirR;
        int file = f_i + dirF;
        if (!board.isInside(rank, file)) return false;
        
        if ((!board.isEmpty(rank, file) 
             && !getColor().isOpposite(board.getPieceAt(rank, file).getColor()))) return false;
        
        MoveType moveType = board.isEmpty(rank, file) ? MoveType.NORMAL : MoveType.CAPTURE;
        moves.add(new Move(new Position(orig_r, orig_f), new Position(rank, file), moveType));
        
        return board.isEmpty(rank, file);
    }

    /** Expands queen moves along diagonals and ranks/files. */
    private void addMoves(Board board, List<Move> moves, int r, int f) {
        // diagonals
        for (int dr : new int[] { -1, 1 }) {
            for (int df : new int[] { -1, 1 }) {
                int r_i = r, f_i = f; 
                while (addOneAdjMove(board, moves, r, f, r_i, f_i, dr, df)) {
                    r_i += dr;
                    f_i += df;
                }
            }
        }
        // verticals
        for (int dr : new int[] { -1, 1 }) {
            int r_i = r;
            while (addOneAdjMove(board, moves, r, f, r_i, f, dr, 0)) {
                r_i += dr;
            }
        }
        // horizontals
        for (int df : new int[] { -1, 1 }) {
            int f_i = f; 
            while (addOneAdjMove(board, moves, r, f, r, f_i, 0, df)) {
                f_i += df;
            }
        }
    }

    /** @return 'Q' symbol for queen in FEN/PGN */
    @Override
    public char getSymbol() { return 'Q'; }

    /** @return "queen" */
    @Override
    public String getName() { return "queen"; }
    
    /** Copy constructor. */
    protected Queen(Queen other) { super(other); }
        
    /** @return deep copy of this queen */
    @Override
    public Piece copy() { return new Queen(this); }
}
