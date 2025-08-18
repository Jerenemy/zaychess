package com.frommzay.chess.model.move;

import java.util.List;

import com.frommzay.chess.model.game.GameState;
import com.frommzay.chess.model.pieces.*;
import com.frommzay.chess.model.util.PlayerColor;
import com.frommzay.chess.model.util.Position;

/**
 * Manages special-move state and validation such as castling and en passant.
 * 
 * Tracks whether rooks and kings have moved (for castling rights),
 * and remembers the en passant target square if a pawn advanced two ranks.
 * Provides helper methods to add special moves to pseudo-legal move lists,
 * validate them, and update flags when moves are applied.
 */
public class SpecialMoveHandler {

    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteKingsideRookMoved;
    private boolean whiteQueensideRookMoved;
    private boolean blackKingsideRookMoved;
    private boolean blackQueensideRookMoved;

    private Position enPassantTarget; // null if not available

    /** Creates a fresh handler with no moves yet made. */
    public SpecialMoveHandler() {
        this.enPassantTarget = null;
        this.whiteKingMoved = false;
        this.blackKingMoved = false;
        this.whiteKingsideRookMoved = false;
        this.whiteQueensideRookMoved = false;
        this.blackKingsideRookMoved = false;
        this.blackQueensideRookMoved = false;
    }

    /** @return the current en passant target square, or null if none is available */
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    /**
     * Adds en passant moves to the given move list if available and valid.
     *
     * @param s                current game state
     * @param pos              the pawn's position
     * @param pseudoLegalMoves list to append moves to
     */
    public void addEnPassantMoves(GameState s, Position pos, List<Move> pseudoLegalMoves) {
        if (enPassantTarget == null) return;

        Move epMove = new Move(pos, enPassantTarget, MoveType.EN_PASSANT);
        if (isEnPassantValid(s, epMove)) {
            pseudoLegalMoves.add(epMove);
        }
    }

    /**
     * Checks if an en passant move is valid given the current state.
     *
     * @param s game state
     * @param m candidate move
     * @return true if it represents a valid en passant capture
     */
    public boolean isEnPassantValid(GameState s, Move m) {
        if (enPassantTarget == null) return false;

        Position from = m.getFromPos();
        Position to = m.getToPos();
        Piece mover = s.getPieceAt(from);
        if (!(mover instanceof Pawn)) return false;
        if (!to.equals(enPassantTarget)) return false;

        int dir = getDir(mover.getColor());
        if (to.getRank() - from.getRank() != dir) return false;
        if (Math.abs(to.getFile() - from.getFile()) != 1) return false;
        if (s.getPieceAt(to) != null) return false;

        Position capturedPos = new Position(from.getRank(), to.getFile());
        Piece captured = s.getPieceAt(capturedPos);
        if (!(captured instanceof Pawn)) return false;
        if (captured.getColor() == mover.getColor()) return false;

        return true;
    }

    /** Returns pawn forward direction based on color. */
    private int getDir(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? -1 : 1;
    }

    /**
     * Updates the en passant target square after a pawn's move.
     * If the pawn advanced two squares from its start rank,
     * sets the midpoint square as the target.
     */
    public void updateEnPassantTarget(GameState s, Move m) {
        this.enPassantTarget = null;

        Position from = m.getFromPos();
        Position to = m.getToPos();

        Piece mover = s.getPieceAt(from);
        if (!(mover instanceof Pawn)) return;

        int fromRank = from.getRank();
        int toRank = to.getRank();
        int file = from.getFile();

        if (Math.abs(toRank - fromRank) == 2 && fromRank == pawnStartRank(mover.getColor())) {
            int midRank = (fromRank + toRank) / 2;
            this.enPassantTarget = new Position(midRank, file);
        }
    }

    private static int pawnStartRank(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? 6 : 1;
    }

    private static int kingRookStartRank(PlayerColor color) {
        return (color == PlayerColor.WHITE) ? 7 : 0;
    }

    /**
     * Adds castling moves for a king at its home square if conditions allow.
     *
     * @param s                game state
     * @param from             king's position
     * @param pseudoLegalMoves list to append moves to
     */
    public void addCastlingMoves(GameState s, Position from, List<Move> pseudoLegalMoves) {
        Piece p = s.getPieceAt(from);
        if (!(p instanceof King)) return;

        if (from.getFile() != 4) return;
        if (from.getRank() != kingRookStartRank(p.getColor())) return;

        Move queenside = new Move(from, new Position(from.getRank(), 2), MoveType.CASTLE);
        Move kingside  = new Move(from, new Position(from.getRank(), 6), MoveType.CASTLE);

        for (Move m : new Move[] { queenside, kingside }) {
            if (isCastlingValid(s, m)) {
                pseudoLegalMoves.add(m);
            }
        }
    }

    /**
     * Validates a castling move according to chess rules.
     *
     * @param s current game state
     * @param m candidate move
     * @return true if castling is legal
     */
    public boolean isCastlingValid(GameState s, Move m) {
        Position from = m.getFromPos();
        Position to = m.getToPos();

        Piece mover = s.getPieceAt(from);
        if (!(mover instanceof King)) return false;

        PlayerColor us = mover.getColor();
        PlayerColor them = us.getOpposite();

        if (from.getRank() != kingRookStartRank(us)) return false;
        if (from.getFile() != 4) return false;

        int toFile = to.getFile();
        if (to.getRank() != from.getRank()) return false;
        boolean kingSide = (toFile == 6);
        boolean queenSide = (toFile == 2);
        if (!(kingSide || queenSide)) return false;

        if (s.getPieceAt(to) != null) return false;

        if (us == PlayerColor.WHITE) {
            if (whiteKingMoved) return false;
            if (kingSide && whiteKingsideRookMoved) return false;
            if (queenSide && whiteQueensideRookMoved) return false;
        } else {
            if (blackKingMoved) return false;
            if (kingSide && blackKingsideRookMoved) return false;
            if (queenSide && blackQueensideRookMoved) return false;
        }

        int rookFile = kingSide ? 7 : 0;
        Position rookPos = new Position(from.getRank(), rookFile);
        Piece rook = s.getPieceAt(rookPos);
        if (!(rook instanceof Rook) || rook.getColor() != us) return false;

        int stepKR = (rookFile > from.getFile()) ? 1 : -1;
        for (int f = from.getFile() + stepKR; f != rookFile; f += stepKR) {
            if (s.getPieceAt(new Position(from.getRank(), f)) != null) return false;
        }

        if (s.getKingOfColor(us).isInCheck(s)) return false;
        int stepK = (toFile > from.getFile()) ? 1 : -1;
        for (int f = from.getFile() + stepK; f != toFile + stepK; f += stepK) {
            Position sq = new Position(from.getRank(), f);
            if (isSquareAttacked(s, sq, them)) return false;
        }

        return true;
    }

    /** Helper: checks whether a square is attacked by the opponent. */
    private boolean isSquareAttacked(GameState s, Position sq, PlayerColor them) {
        GameState probe = s.copy();
        King king = probe.getKingOfColor(them.getOpposite());
        Move m = new Move(king.getPos(), sq, MoveType.NORMAL);
        probe.applyMove(m);
        probe.changeTurn();
        return king.isInCheck(probe);
    }

    /** Prints internal flags (debug helper). */
    public void printHasMovedFlags() {
        System.out.println(
            "whiteKingMoved=" + whiteKingMoved +
            " blackKingMoved=" + blackKingMoved +
            " whiteKingsideRookMoved=" + whiteKingsideRookMoved +
            " whiteQueensideRookMoved=" + whiteQueensideRookMoved +
            " blackKingsideRookMoved=" + blackKingsideRookMoved +
            " blackQueensideRookMoved=" + blackQueensideRookMoved
        );
    }

    /**
     * Updates has-moved flags after a move is applied.
     *
     * @param s game state
     * @param m move that was just made
     */
    public void updateHasMovedFlags(GameState s, Move m) {
        Position from = m.getFromPos();
        Piece mover = s.getPieceAt(from);

        PlayerColor us = mover.getColor();
        boolean isWhite = (us == PlayerColor.WHITE);

        if (mover instanceof King) {
            if (isWhite) whiteKingMoved = true; else blackKingMoved = true;
        }

        if (mover instanceof Rook && from.getRank() == kingRookStartRank(mover.getColor())) {
            if (from.getFile() == 0) {
                if (isWhite) whiteQueensideRookMoved = true; else blackQueensideRookMoved = true;
            }
            else if (from.getFile() == 7) {
                if (isWhite) whiteKingsideRookMoved = true; else blackKingsideRookMoved = true;
            }
        }
    }

    /** Moves the rook to its correct square after castling. */
    public void moveRookDuringCastle(GameState state, Move m) {
        int r = m.getFromPos().getRank();
        if (m.getToPos().getFile() == 6) {
            movePieceFromTo(state, r, 7, r, 5);
        }
        else if (m.getToPos().getFile() == 2) {
            movePieceFromTo(state, r, 0, r, 3);
        }
    }

    /** Helper: moves a piece between coordinates. */
    public void movePieceFromTo(GameState state, int from_rank, int from_file, int to_rank, int to_file) {
        state.setPieceAt(to_rank, to_file, state.getPieceAt(from_rank, from_file));
        state.setPieceAt(from_rank, from_file, null);
    }

    /** Helper: moves a piece between positions. */
    public void movePieceFromTo(GameState state, Position from, Position to) {
        state.setPieceAt(to, state.getPieceAt(from));
        state.setPieceAt(from, null);
    }

    /** Deep-copy constructor. */
    public SpecialMoveHandler(SpecialMoveHandler other) {
        this.enPassantTarget = other.enPassantTarget;
        this.whiteKingMoved = other.whiteKingMoved;
        this.blackKingMoved = other.blackKingMoved;
        this.whiteKingsideRookMoved = other.whiteKingsideRookMoved;
        this.whiteQueensideRookMoved = other.whiteQueensideRookMoved;
        this.blackKingsideRookMoved = other.blackKingsideRookMoved;
        this.blackQueensideRookMoved = other.blackQueensideRookMoved;
    }

    /** @return a deep copy of this handler */
    public SpecialMoveHandler copy() {
        return new SpecialMoveHandler(this);
    }

    /**
     * Copies state from another handler into this one.
     * 
     * Copies en passant target and castling/rook/king movement flags.
     *
     * @param other the handler to copy from
     */
    public void copyFrom(SpecialMoveHandler other) {
        this.enPassantTarget = (other.enPassantTarget == null ? null
            : new Position(other.enPassantTarget));
        this.whiteKingMoved = other.whiteKingMoved;
        this.blackKingMoved = other.blackKingMoved;
        this.whiteKingsideRookMoved = other.whiteKingsideRookMoved;
        this.whiteQueensideRookMoved = other.whiteQueensideRookMoved;
        this.blackKingsideRookMoved = other.blackKingsideRookMoved;
        this.blackQueensideRookMoved = other.blackQueensideRookMoved;
    }
}
