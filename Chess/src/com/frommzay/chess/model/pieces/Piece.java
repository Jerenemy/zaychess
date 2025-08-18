package com.frommzay.chess.model.pieces;

import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;

import java.util.List;

import com.frommzay.chess.model.board.Board;
import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.move.Move;
import com.frommzay.chess.model.move.MoveType;

/**
 * Abstract base class for all chess pieces.
 * 
 * Stores color and position, and defines the core API
 * for generating pseudo-legal moves, copying pieces, and
 * retrieving metadata such as symbol and name.
 */
public abstract class Piece {
    
    private final PlayerColor color;
    private Position pos;
    
    /** Creates a piece of the given color at a position. */
    protected Piece(PlayerColor color, Position pos) {
        this.color = color;
        this.pos = pos;
    }
    
    /** Creates a piece of the given color at a specific rank/file. */
    protected Piece(PlayerColor color, int rank, int file) {
        this.color = color;
        this.pos = new Position(rank, file);
    }
    
    /** @return the piece's color */
    public PlayerColor getColor() { return color; }
    
    /** @return the piece's current position */
    public Position getPos() { return pos; }
    
    /**
     * Returns all pseudo-legal moves for this piece, ignoring checks.
     * Legal filtering is performed by {@link com.frommzay.chess.model.move.MoveValidator}.
     *
     * @param gameState current game state
     * @param fromPos   position of this piece
     * @return list of pseudo-legal moves
     */
    public abstract List<Move> generatePseudoLegalMoves(GameState gameState, Position fromPos);

    /**
     * Checks if it is this piece's turn to move.
     * @param gameState current game state
     * @return true if piece color matches current turn
     */
    public Boolean isTurn(GameState gameState) {
        return (gameState.getTurn() == getColor()) ;
    }
    
    /** Copy constructor performing a deep copy of position. */
    protected Piece(Piece other) {
        this.color = other.color;
        this.pos   = new Position(other.pos.getRank(), other.pos.getFile());
    }
    
    /** @return a deep copy of this piece */
    public abstract Piece copy();
    
    /** @return one-character symbol used in FEN/PGN notation */
    public abstract char getSymbol();
    
    /** @return lowercase string name (e.g. "pawn", "bishop") */
    public abstract String getName();
    
    /**
     * @return filename-friendly name of piece for assets
     *         (e.g. "white_p", "black_q").
     */
    public String getPngName() {
        return this.getColor().toString().toLowerCase() + '_' + this.getSymbol(); 
    }
    
    /**
     * Debug helper: generates moves to every square,
     * ignoring legality, useful for testing.
     */
    public void generateDefaultAllMoves(Board board, Position fromPos, List<Move> moves) {
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                if (!board.isInside(r, f)) continue;
                if (!board.isEmpty(r, f) &&
                    getColor() == board.getPieceAt(r, f).getColor()) continue;
                moves.add(new Move(
                    new Position(r,f),
                    new Position(r, f),
                    MoveType.NORMAL
                ));
            }
        }
        return;
    }
    
    /** Updates this piece's coordinates. */
    public void updateCoords(Position newPos) {
        pos = newPos;
    }
    
    /** Updates this piece's coordinates. */
    public void updateCoords(int rank, int file) {
        pos = new Position(rank, file);
    }
}
